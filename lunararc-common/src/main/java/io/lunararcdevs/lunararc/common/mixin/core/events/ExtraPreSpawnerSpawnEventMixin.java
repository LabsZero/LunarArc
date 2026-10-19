package io.lunararcdevs.lunararc.common.mixin.core.events;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.lunararcdevs.lunararc.common.event.LunarArcMobEvents;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.storage.ValueInput;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** PreSpawnerSpawnEvent: a cancelled spawn attempt makes the spawner wait and try again, as when loading fails. */
@Mixin(BaseSpawner.class)
public abstract class ExtraPreSpawnerSpawnEventMixin {
    @WrapOperation(method = "serverTick", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType;loadEntityRecursive(Lnet/minecraft/world/level/storage/ValueInput;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/EntitySpawnReason;Lnet/minecraft/world/entity/EntityProcessor;)Lnet/minecraft/world/entity/Entity;"))
    private @Nullable Entity lunararc$preSpawn(ValueInput input, Level level, EntitySpawnReason reason, net.minecraft.world.entity.EntityProcessor postLoad,
            Operation<Entity> original, @Local(ordinal = 0) Vec3 spawnAt, @Local Optional<EntityType<?>> type, @Local(argsOnly = true) BlockPos spawner) {
        if (level instanceof ServerLevel server && type.isPresent() && !LunarArcMobEvents.preSpawnerSpawn(server, spawnAt, type.get(), spawner)) {
            return null;
        }
        return original.call(input, level, reason, postLoad);
    }
}
