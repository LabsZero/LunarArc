package io.lunararcdevs.lunararc.common.mixin.core.events;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.lunararcdevs.lunararc.common.event.LunarArcMobEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

/** PreCreatureSpawnEvent for mobs spawned through EntityType.spawn. */
@Mixin(EntityType.class)
public abstract class ExtraPreCreatureSpawnEventMixin<T extends Entity> {
    @WrapMethod(method = "spawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/PostSpawnProcessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/EntitySpawnReason;ZZ)Lnet/minecraft/world/entity/Entity;", require = 0)
    private @Nullable T lunararc$preSpawn(ServerLevel level, @Nullable net.minecraft.world.entity.PostSpawnProcessor<T> consumer, BlockPos pos, EntitySpawnReason reason,
            boolean offsetY, boolean offsetMore, Operation<T> original) {
        if (!LunarArcMobEvents.preCreatureSpawn(level, pos, (EntityType<?>) (Object) this, LunarArcMobEvents.bukkitReason(reason))) return null;
        return original.call(level, consumer, pos, reason, offsetY, offsetMore);
    }
}
