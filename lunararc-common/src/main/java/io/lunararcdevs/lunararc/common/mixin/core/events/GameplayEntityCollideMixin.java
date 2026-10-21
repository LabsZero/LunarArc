package io.lunararcdevs.lunararc.common.mixin.core.events;

import io.lunararcdevs.lunararc.common.event.LunarArcGameplayEvents;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** EntityCollideWithEntityEvent for plain entity pushes. */
@Mixin(Entity.class)
public abstract class GameplayEntityCollideMixin {
    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void lunararc$collide(Entity other, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self.level().isClientSide() || self.isPassengerOfSameVehicle(other) || other.noPhysics || self.noPhysics) return;
        if (!LunarArcGameplayEvents.collide(self, other)) ci.cancel();
    }
}
