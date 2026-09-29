package io.lunararcdevs.lunararc.common.mixin.core.entity;

import io.lunararcdevs.lunararc.common.bridge.EntityBridge;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.CraftEquipmentSlot;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorStand.class)
public abstract class ArmorStandMixin implements io.lunararcdevs.lunararc.common.bridge.entity.MovementLockBridge {
    public boolean canMove = true;
    public boolean canTick = true;
    public boolean canTickSetByAPI = false;

    @Override public boolean lunararc$canMove() { return this.canMove; }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void lunararc$skipTick(CallbackInfo ci) {
        if (!this.canTick) ci.cancel();
    }

    @Inject(method = "swapItem", cancellable = true, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;hasInfiniteMaterials()Z"))
    private void lunararc$manipulate(Player player, EquipmentSlot slot, ItemStack held, InteractionHand hand, CallbackInfoReturnable<Boolean> cir) {
        ArmorStand self = (ArmorStand) (Object) this;
        if (!(((EntityBridge) player).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Player bukkitPlayer)
                || !(((EntityBridge) self).lunararc$getBukkitEntity() instanceof org.bukkit.entity.ArmorStand stand)) {
            return;
        }
        var event = new org.bukkit.event.player.PlayerArmorStandManipulateEvent(bukkitPlayer, stand,
                CraftItemStack.asCraftMirror(held), CraftItemStack.asCraftMirror(self.getItemBySlot(slot)),
                CraftEquipmentSlot.getSlot(slot), CraftEquipmentSlot.getHand(hand));
        org.bukkit.Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) cir.setReturnValue(true);
    }
}
