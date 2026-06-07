package io.ampznetwork.lunararc.common.mixin.core.world;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public abstract class LevelEntityMixin {

    /**
     * Fire EntitySpawnEvent / CreatureSpawnEvent when an entity is added to the world.
     * addFreshEntity returns boolean in 1.21.1, so we must use CallbackInfoReturnable.
     */
    @Inject(method = "addFreshEntity", at = @At("HEAD"), cancellable = true)
    private void lunararc$onEntityAdd(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        try {
            org.bukkit.Server server = org.bukkit.Bukkit.getServer();
            if (server == null) return;

            if (!(entity instanceof io.ampznetwork.lunararc.common.bridge.EntityBridge bridge)) return;
            org.bukkit.entity.Entity bukkitEntity = bridge.lunararc$getBukkitEntity();
            if (bukkitEntity == null) return;

            if (entity instanceof net.minecraft.world.entity.Mob) {
                org.bukkit.event.entity.CreatureSpawnEvent event =
                        new org.bukkit.event.entity.CreatureSpawnEvent(
                                (org.bukkit.entity.LivingEntity) bukkitEntity,
                                org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.DEFAULT);
                server.getPluginManager().callEvent(event);
                if (event.isCancelled()) {
                    cir.setReturnValue(false);
                }
            } else {
                org.bukkit.event.entity.EntitySpawnEvent event =
                        new org.bukkit.event.entity.EntitySpawnEvent(bukkitEntity);
                server.getPluginManager().callEvent(event);
                if (event.isCancelled()) {
                    cir.setReturnValue(false);
                }
            }
        } catch (Throwable t) {
            // Never let Bukkit event errors crash entity spawning
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void lunararc$onTick(CallbackInfo ci) {
        // Placeholder for future per-tick world hooks
    }
}
