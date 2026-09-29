package io.lunararcdevs.lunararc.common.mixin.core.world.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(VineBlock.class)
public abstract class VineBlockMixin {
    @Unique private static final String SET_BLOCK =
            "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z";

    @WrapOperation(method = "randomTick", at = {
            @At(value = "INVOKE", ordinal = 0, target = SET_BLOCK), @At(value = "INVOKE", ordinal = 1, target = SET_BLOCK),
            @At(value = "INVOKE", ordinal = 2, target = SET_BLOCK), @At(value = "INVOKE", ordinal = 3, target = SET_BLOCK),
            @At(value = "INVOKE", ordinal = 4, target = SET_BLOCK), @At(value = "INVOKE", ordinal = 7, target = SET_BLOCK),
            @At(value = "INVOKE", ordinal = 8, target = SET_BLOCK)})
    private boolean lunararc$spread(ServerLevel level, BlockPos target, BlockState state, int flags, Operation<Boolean> original,
            @Local(argsOnly = true) BlockPos source) {
        return CraftEventFactory.handleBlockSpreadEvent(level, source, target, state, flags, (p, s, f) -> original.call(level, p, s, f));
    }

    @WrapOperation(method = "randomTick", at = {
            @At(value = "INVOKE", ordinal = 5, target = SET_BLOCK), @At(value = "INVOKE", ordinal = 6, target = SET_BLOCK)})
    private boolean lunararc$grow(ServerLevel level, BlockPos pos, BlockState state, int flags, Operation<Boolean> original) {
        return CraftEventFactory.handleBlockGrowEvent(level, pos, state, flags, (p, s, f) -> original.call(level, p, s, f));
    }
}
