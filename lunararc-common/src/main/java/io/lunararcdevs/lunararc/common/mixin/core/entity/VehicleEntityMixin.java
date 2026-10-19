package io.lunararcdevs.lunararc.common.mixin.core.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.lunararcdevs.lunararc.common.bridge.EntityBridge;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VehicleEntity.class)
public abstract class VehicleEntityMixin {

    @WrapMethod(method = "hurtServer")
    private boolean lunararc$vehicleDamage(net.minecraft.server.level.ServerLevel level, DamageSource source, float amount, Operation<Boolean> original) {
        VehicleEntity vehicle = (VehicleEntity) (Object) this;
        if (vehicle.isRemoved() || vehicle.isInvulnerableToBase(source)) {
            return original.call(level, source, amount);
        }
        Object bukkit = ((EntityBridge) vehicle).lunararc$getBukkitEntity();
        if (!(bukkit instanceof org.bukkit.entity.Vehicle bukkitVehicle)) {
            return original.call(level, source, amount);
        }
        org.bukkit.entity.Entity attacker = source.getEntity() == null
                ? null
                : ((EntityBridge) source.getEntity()).lunararc$getBukkitEntity();
        org.bukkit.event.vehicle.VehicleDamageEvent event =
                new org.bukkit.event.vehicle.VehicleDamageEvent(bukkitVehicle, new org.bukkit.craftbukkit.damage.CraftDamageSource(source), attacker, amount);
        org.bukkit.Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }
        return original.call(level, source, (float) event.getDamage());
    }

    @Inject(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/vehicle/VehicleEntity;discard()V"), cancellable = true, require = 1)
    private void lunararc$creativeVehicleDestroy(net.minecraft.server.level.ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!this.lunararc$allowVehicleDestroy(source)) {
            ((VehicleEntity) (Object) this).setDamage(40.0F);
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/vehicle/VehicleEntity;destroy(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;)V"), cancellable = true, require = 1)
    private void lunararc$normalVehicleDestroy(net.minecraft.server.level.ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!this.lunararc$allowVehicleDestroy(source)) {
            ((VehicleEntity) (Object) this).setDamage(40.0F);
            cir.setReturnValue(true);
        }
    }

    private boolean lunararc$allowVehicleDestroy(DamageSource source) {
        VehicleEntity vehicle = (VehicleEntity) (Object) this;
        Object bukkit = ((EntityBridge) vehicle).lunararc$getBukkitEntity();
        if (!(bukkit instanceof org.bukkit.entity.Vehicle bukkitVehicle)) {
            return true;
        }
        org.bukkit.entity.Entity attacker = source.getEntity() == null
                ? null
                : ((EntityBridge) source.getEntity()).lunararc$getBukkitEntity();
        org.bukkit.event.vehicle.VehicleDestroyEvent event =
                new org.bukkit.event.vehicle.VehicleDestroyEvent(bukkitVehicle, new org.bukkit.craftbukkit.damage.CraftDamageSource(source), attacker);
        org.bukkit.Bukkit.getPluginManager().callEvent(event);
        return !event.isCancelled();
    }
}
