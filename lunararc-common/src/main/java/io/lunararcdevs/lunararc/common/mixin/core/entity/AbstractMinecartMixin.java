package io.lunararcdevs.lunararc.common.mixin.core.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.lunararcdevs.lunararc.common.bridge.EntityBridge;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import org.bukkit.Location;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractMinecart.class)
public abstract class AbstractMinecartMixin {
    public double maxSpeed = 0.4D;
    public boolean slowWhenEmpty = true;
    @Unique private double lunararc$derailedX = 0.5D;
    @Unique private double lunararc$derailedY = 0.5D;
    @Unique private double lunararc$derailedZ = 0.5D;
    @Unique private double lunararc$flyingX = 0.95D;
    @Unique private double lunararc$flyingY = 0.95D;
    @Unique private double lunararc$flyingZ = 0.95D;

    public org.bukkit.util.Vector getDerailedVelocityMod() {
        return new org.bukkit.util.Vector(this.lunararc$derailedX, this.lunararc$derailedY, this.lunararc$derailedZ);
    }

    public void setDerailedVelocityMod(org.bukkit.util.Vector derailedVelocityMod) {
        this.lunararc$derailedX = derailedVelocityMod.getX();
        this.lunararc$derailedY = derailedVelocityMod.getY();
        this.lunararc$derailedZ = derailedVelocityMod.getZ();
    }

    public org.bukkit.util.Vector getFlyingVelocityMod() {
        return new org.bukkit.util.Vector(this.lunararc$flyingX, this.lunararc$flyingY, this.lunararc$flyingZ);
    }

    public void setFlyingVelocityMod(org.bukkit.util.Vector flyingVelocityMod) {
        this.lunararc$flyingX = flyingVelocityMod.getX();
        this.lunararc$flyingY = flyingVelocityMod.getY();
        this.lunararc$flyingZ = flyingVelocityMod.getZ();
    }

    @org.spongepowered.asm.mixin.Shadow protected abstract double getMaxSpeed();

    @Inject(method = "getMaxSpeed", at = @At("HEAD"), cancellable = true)
    private void lunararc$maxSpeed(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Double> cir) {
        AbstractMinecart self = (AbstractMinecart) (Object) this;
        cir.setReturnValue(self.isInWater() ? this.maxSpeed / 2.0D : this.maxSpeed);
    }

    @Inject(method = "applyNaturalSlowdown", at = @At("HEAD"), cancellable = true)
    private void lunararc$slowdown(CallbackInfo ci) {
        if (this.slowWhenEmpty) return;
        AbstractMinecart self = (AbstractMinecart) (Object) this;
        net.minecraft.world.phys.Vec3 velocity = self.getDeltaMovement().multiply(0.997D, 0.0D, 0.997D);
        if (self.isInWater()) velocity = velocity.scale(0.95D);
        self.setDeltaMovement(velocity);
        ci.cancel();
    }

    @Inject(method = "comeOffTrack", at = @At("HEAD"), cancellable = true)
    private void lunararc$comeOffTrack(CallbackInfo ci) {
        AbstractMinecart self = (AbstractMinecart) (Object) this;
        double max = this.getMaxSpeed();
        net.minecraft.world.phys.Vec3 velocity = self.getDeltaMovement();
        self.setDeltaMovement(net.minecraft.util.Mth.clamp(velocity.x, -max, max), velocity.y,
                net.minecraft.util.Mth.clamp(velocity.z, -max, max));
        if (self.onGround()) {
            velocity = self.getDeltaMovement();
            self.setDeltaMovement(new net.minecraft.world.phys.Vec3(velocity.x * this.lunararc$derailedX,
                    velocity.y * this.lunararc$derailedY, velocity.z * this.lunararc$derailedZ));
        }
        self.move(net.minecraft.world.entity.MoverType.SELF, self.getDeltaMovement());
        if (!self.onGround()) {
            velocity = self.getDeltaMovement();
            self.setDeltaMovement(new net.minecraft.world.phys.Vec3(velocity.x * this.lunararc$flyingX,
                    velocity.y * this.lunararc$flyingY, velocity.z * this.lunararc$flyingZ));
        }
        ci.cancel();
    }

    @Unique private Location lunararc$tickStart;

    @Inject(method = "tick", at = @At("HEAD"))
    private void lunararc$captureStart(CallbackInfo ci) {
        AbstractMinecart self = (AbstractMinecart) (Object) this;
        this.lunararc$tickStart = self.level().isClientSide ? null : ((EntityBridge) self).lunararc$getBukkitEntity().getLocation();
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", ordinal = 1, shift = At.Shift.AFTER,
            target = "Lnet/minecraft/world/entity/vehicle/AbstractMinecart;setRot(FF)V"))
    private void lunararc$vehicleEvents(CallbackInfo ci) {
        if (this.lunararc$tickStart == null) return;
        CraftEventFactory.callVehicleUpdateAndMove((AbstractMinecart) (Object) this, this.lunararc$tickStart);
        this.lunararc$tickStart = null;
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;push(Lnet/minecraft/world/entity/Entity;)V"))
    private void lunararc$collidePush(Entity entity, Entity other, Operation<Void> original) {
        Entity self = (Entity) (Object) this;
        if (CraftEventFactory.callVehicleEntityCollisionCancelled(self, entity == self ? other : entity)) return;
        original.call(entity, other);
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;startRiding(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean lunararc$collideRide(Entity entity, Entity vehicle, Operation<Boolean> original) {
        if (CraftEventFactory.callVehicleEntityCollisionCancelled((Entity) (Object) this, entity)) return false;
        return original.call(entity, vehicle);
    }
}
