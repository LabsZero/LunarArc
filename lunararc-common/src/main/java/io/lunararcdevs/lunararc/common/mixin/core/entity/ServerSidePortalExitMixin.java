package io.lunararcdevs.lunararc.common.mixin.core.entity;

import io.lunararcdevs.lunararc.common.event.LunarArcServerSideEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.portal.TeleportTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** EntityPortalExitEvent: plugins may move an entity leaving a portal, or change its exit velocity. */
@Mixin(Entity.class)
public abstract class ServerSidePortalExitMixin {
    @ModifyVariable(method = "teleport(Lnet/minecraft/world/level/portal/TeleportTransition;)Lnet/minecraft/world/entity/Entity;",
            at = @At("HEAD"), argsOnly = true, require = 0)
    private TeleportTransition lunararc$portalExit(TeleportTransition transition) {
        Entity self = (Entity) (Object) this;
        return self.portalProcess == null ? transition : LunarArcServerSideEvents.portalExit(self, transition);
    }
}
