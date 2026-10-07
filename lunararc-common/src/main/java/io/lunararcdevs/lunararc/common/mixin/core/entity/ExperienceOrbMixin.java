package io.lunararcdevs.lunararc.common.mixin.core.entity;

import io.lunararcdevs.lunararc.common.bridge.EntityBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Paper/Bukkit XP pickup event without replacing orb pickup or mending logic. */
@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbMixin {
    public java.util.UUID sourceEntityId;
    public java.util.UUID triggerEntityId;
    public org.bukkit.entity.ExperienceOrb.SpawnReason spawnReason = org.bukkit.entity.ExperienceOrb.SpawnReason.UNKNOWN;


    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true, require = 0)
    private void lunararc$pickupExperienceEvent(Player player, CallbackInfo ci) {
        if (io.lunararcdevs.lunararc.common.event.LunarArcPaperEvents.pickupExperience(player, (ExperienceOrb) (Object) this)) ci.cancel();
    }

    @org.spongepowered.asm.mixin.Shadow
    protected abstract int repairPlayerItems(ServerPlayer player, int experience);

    @Inject(method = "repairPlayerItems", at = @At("HEAD"), cancellable = true, require = 0)
    private void lunararc$mend(ServerPlayer player, int experience, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Integer> cir) {
        if (!io.lunararcdevs.lunararc.common.event.LunarArcPaperEvents.mendListened()) return;
        java.util.Optional<net.minecraft.world.item.enchantment.EnchantedItemInUse> optional =
                net.minecraft.world.item.enchantment.EnchantmentHelper.getRandomItemWith(
                        net.minecraft.world.item.enchantment.EnchantmentEffectComponents.REPAIR_WITH_XP,
                        player, net.minecraft.world.item.ItemStack::isDamaged);
        if (optional.isEmpty()) return;
        net.minecraft.world.item.ItemStack stack = optional.get().itemStack();
        int durability = net.minecraft.world.item.enchantment.EnchantmentHelper.modifyDurabilityToRepairFromXp(player.serverLevel(), stack, experience);
        int repair = io.lunararcdevs.lunararc.common.event.LunarArcPaperEvents.mend((ServerPlayer) player,
                (ExperienceOrb) (Object) this, stack, optional.get().inSlot(), Math.min(durability, stack.getDamageValue()), experience);
        if (repair < 0) {
            cir.setReturnValue(experience);
            return;
        }
        stack.setDamageValue(stack.getDamageValue() - repair);
        int remaining = 0;
        if (repair > 0 && durability > 0) {
            int left = experience - repair * experience / durability;
            if (left > 0) remaining = this.repairPlayerItems(player, left);
        }
        cir.setReturnValue(remaining);
    }

    @Redirect(
            method = "playerTouch",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;giveExperiencePoints(I)V"),
            require = 0)
    private void lunararc$playerExpChange(Player player, int amount) {
        ExperienceOrb self = (ExperienceOrb) (Object) this;
        int awarded = amount;
        if (player instanceof ServerPlayer && !self.level().isClientSide) {
            Object bukkitPlayer = ((EntityBridge) player).lunararc$getBukkitEntity();
            Object bukkitOrb = ((EntityBridge) self).lunararc$getBukkitEntity();
            if (bukkitPlayer instanceof org.bukkit.entity.Player bp
                    && bukkitOrb instanceof org.bukkit.entity.ExperienceOrb bo) {
                org.bukkit.event.player.PlayerExpChangeEvent event =
                        new org.bukkit.event.player.PlayerExpChangeEvent(bp, bo, amount);
                org.bukkit.Bukkit.getPluginManager().callEvent(event);
                awarded = event.getAmount();
            }
        }
        player.giveExperiencePoints(awarded);
    }
}
