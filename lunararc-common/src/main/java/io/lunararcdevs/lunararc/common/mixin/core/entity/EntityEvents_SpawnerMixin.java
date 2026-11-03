package io.lunararcdevs.lunararc.common.mixin.core.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.lunararcdevs.lunararc.common.event.LunarArcMoreEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BaseSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BaseSpawner.class)
public abstract class EntityEvents_SpawnerMixin {
    @WrapOperation(method = "serverTick", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;tryAddFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean lunararc$spawn(ServerLevel level, Entity entity, Operation<Boolean> original,
            @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) BlockPos pos) {
        return LunarArcMoreEvents.spawnerSpawn(entity, level, pos) && original.call(level, entity);
    }
}
