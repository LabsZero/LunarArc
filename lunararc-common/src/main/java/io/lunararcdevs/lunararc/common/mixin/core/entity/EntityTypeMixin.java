package io.lunararcdevs.lunararc.common.mixin.core.entity;

import io.lunararcdevs.lunararc.common.bridge.EntityBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Captures the loader-owned EntitySpawnReason at EntityType creation time and carries it forward to
 * the later Bukkit spawn event. More specific LunarArc hooks may replace DEFAULT-like reasons
 * before the entity is actually added to the ServerLevel.
 */
@Mixin(EntityType.class)
public abstract class EntityTypeMixin<T extends Entity> {
    @Inject(
            method = "create(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/PostSpawnProcessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/EntitySpawnReason;ZZ)Lnet/minecraft/world/entity/Entity;",
            at = @At("RETURN"),
            require = 0)
    private void lunararc$captureSpawnType(
            ServerLevel level,
            net.minecraft.world.entity.PostSpawnProcessor<T> consumer,
            BlockPos pos,
            EntitySpawnReason spawnType,
            boolean shouldOffsetY,
            boolean shouldOffsetYMore,
            CallbackInfoReturnable<T> cir) {
        T entity = cir.getReturnValue();
        if (entity == null) return;
        EntityBridge bridge = (EntityBridge) entity;
        if (bridge.lunararc$getSpawnReason() == null) {
            CreatureSpawnEvent.SpawnReason override = io.lunararcdevs.lunararc.common.event.LunarArcSpawnReasons.get();
            bridge.lunararc$setSpawnReason(override != null ? override : lunararc$toBukkitReason(spawnType));
        }
    }

    private static CreatureSpawnEvent.SpawnReason lunararc$toBukkitReason(EntitySpawnReason type) {
        // These are the 1.21.1 vanilla spawn-type families. Special transforms such as
        // drowned/infection/cure remain free to replace the generic CONVERSION reason later.
        return switch (type) {
            case NATURAL -> CreatureSpawnEvent.SpawnReason.NATURAL;
            case CHUNK_GENERATION, STRUCTURE -> CreatureSpawnEvent.SpawnReason.CHUNK_GEN;
            case SPAWNER -> CreatureSpawnEvent.SpawnReason.SPAWNER;
            case BREEDING -> CreatureSpawnEvent.SpawnReason.BREEDING;
            case JOCKEY -> CreatureSpawnEvent.SpawnReason.JOCKEY;
            case REINFORCEMENT -> CreatureSpawnEvent.SpawnReason.REINFORCEMENTS;
            case BUCKET -> CreatureSpawnEvent.SpawnReason.DEFAULT; // Paper 1.21.1 has no BUCKET SpawnReason
            case SPAWN_ITEM_USE -> CreatureSpawnEvent.SpawnReason.SPAWNER_EGG;
            case COMMAND -> CreatureSpawnEvent.SpawnReason.COMMAND;
            case DISPENSER -> CreatureSpawnEvent.SpawnReason.DISPENSE_EGG;
            case PATROL -> CreatureSpawnEvent.SpawnReason.PATROL;
            case TRIAL_SPAWNER -> CreatureSpawnEvent.SpawnReason.TRIAL_SPAWNER;
            default -> CreatureSpawnEvent.SpawnReason.DEFAULT;
        };
    }
}
