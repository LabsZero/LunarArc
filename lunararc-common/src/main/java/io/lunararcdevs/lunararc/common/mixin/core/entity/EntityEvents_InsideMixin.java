package io.lunararcdevs.lunararc.common.mixin.core.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.lunararcdevs.lunararc.common.event.LunarArcMoreEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public abstract class EntityEvents_InsideMixin {
    @WrapOperation(method = "checkInsideBlocks", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;entityInside(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;)V"))
    private void lunararc$inside(BlockState state, Level level, BlockPos pos, Entity entity, Operation<Void> original) {
        if (LunarArcMoreEvents.inside(entity, level, pos)) original.call(state, level, pos, entity);
    }
}
