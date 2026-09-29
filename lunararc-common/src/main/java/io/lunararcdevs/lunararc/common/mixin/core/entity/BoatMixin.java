package io.lunararcdevs.lunararc.common.mixin.core.entity;

import io.lunararcdevs.lunararc.common.bridge.EntityBridge;
import net.minecraft.world.entity.vehicle.Boat;
import org.bukkit.Location;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Boat.class)
public abstract class BoatMixin {
    public boolean landBoats = false;
    public double maxSpeed = 0.4D;
    public double occupiedDeceleration = 0.2D;
    public double unoccupiedDeceleration = -1.0D;

    @Unique private Location lunararc$lastLocation;

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/vehicle/Boat;tickBubbleColumn()V"))
    private void lunararc$vehicleEvents(CallbackInfo ci) {
        Boat self = (Boat) (Object) this;
        if (self.level().isClientSide) return;
        CraftEventFactory.callVehicleUpdateAndMove(self, this.lunararc$lastLocation);
        this.lunararc$lastLocation = ((EntityBridge) self).lunararc$getBukkitEntity().getLocation();
    }
}
