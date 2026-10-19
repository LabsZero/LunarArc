package io.lunararcdevs.lunararc.common.mixin.core.entity;

import io.lunararcdevs.lunararc.common.LunarArcServerAccess;
import io.lunararcdevs.lunararc.common.bridge.EntityBridge;
import io.lunararcdevs.lunararc.common.bridge.LivingEntityBridge;
import io.lunararcdevs.lunararc.common.bridge.PlayerExhaustionBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Player actions whose hooks moved from Player to ServerPlayer: jump exhaustion, peaceful regeneration and item drops. */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerActionsMixin {
    @Inject(method = "jumpFromGround", require = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;causeFoodExhaustion(F)V"))
    private void lunararc$jumpExhaustion(CallbackInfo ci) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        ((PlayerExhaustionBridge) self).lunararc$pushExhaustionReason(self.isSprinting()
                ? org.bukkit.event.entity.EntityExhaustionEvent.ExhaustionReason.JUMP_SPRINT
                : org.bukkit.event.entity.EntityExhaustionEvent.ExhaustionReason.JUMP);
    }

    @Inject(method = "tickRegeneration", require = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;heal(F)V"))
    private void lunararc$peacefulRegen(CallbackInfo ci) {
        ((LivingEntityBridge) this).lunararc$pushHealReason(org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason.REGEN, false);
    }

    @Inject(method = "drop(Lnet/minecraft/world/item/ItemStack;ZLnet/minecraft/util/Prediction;)Lnet/minecraft/world/entity/item/ItemEntity;", require = 1,
            at = @At("RETURN"), cancellable = true)
    private void lunararc$onDrop(ItemStack droppedStack, boolean traceItem, net.minecraft.util.Prediction prediction, CallbackInfoReturnable<ItemEntity> cir) {
        ItemEntity dropped = cir.getReturnValue();
        if (dropped == null || droppedStack.isEmpty()) return;
        ServerPlayer serverPlayer = (ServerPlayer) (Object) this;
        Object bukkitPlayerObject = ((EntityBridge) serverPlayer).lunararc$getBukkitEntity();
        Object bukkitItemObject = ((EntityBridge) dropped).lunararc$getBukkitEntity();
        if (!(bukkitPlayerObject instanceof org.bukkit.entity.Player bukkitPlayer)
                || !(bukkitItemObject instanceof org.bukkit.entity.Item bukkitItem)) {
            return;
        }

        org.bukkit.event.player.PlayerDropItemEvent event = new org.bukkit.event.player.PlayerDropItemEvent(bukkitPlayer, bukkitItem);
        LunarArcServerAccess.getCraftServer(serverPlayer.level().getServer()).getPluginManager().callEvent(event);
        if (!event.isCancelled()) return;

        dropped.discard();
        org.bukkit.inventory.ItemStack restore = bukkitItem.getItemStack();
        if (traceItem) {
            org.bukkit.inventory.ItemStack current = bukkitPlayer.getInventory().getItemInMainHand();
            if (current.getType().isAir()) {
                bukkitPlayer.getInventory().setItemInMainHand(restore);
            } else if (current.isSimilar(restore)
                    && current.getAmount() < current.getMaxStackSize()
                    && restore.getAmount() == 1) {
                current.setAmount(current.getAmount() + 1);
                bukkitPlayer.getInventory().setItemInMainHand(current);
            } else {
                bukkitPlayer.getInventory().addItem(restore);
            }
        } else {
            bukkitPlayer.getInventory().addItem(restore);
        }
        cir.setReturnValue(null);
    }
}
