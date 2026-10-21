package io.lunararcdevs.lunararc.common.mixin.core.events;

import io.lunararcdevs.lunararc.common.event.LunarArcGameplayEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** EntityCollideWithEntityEvent for minecarts, which override the plain entity push. */
@Mixin(AbstractMinecart.class)
public abstract class GameplayMinecartCollideMixin {
    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true, require = 1)
    private void lunararc$collide(Entity other, CallbackInfo ci) {
        AbstractMinecart self = (AbstractMinecart) (Object) this;
        if (self.level().isClientSide() || other.noPhysics || self.noPhysics || self.hasPassenger(other)) return;
        if (!LunarArcGameplayEvents.collide(self, other)) ci.cancel();
    }
}
