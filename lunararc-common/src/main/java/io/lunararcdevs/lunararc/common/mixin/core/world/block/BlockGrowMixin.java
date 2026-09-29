package io.lunararcdevs.lunararc.common.mixin.core.world.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({CropBlock.class, NetherWartBlock.class, CocoaBlock.class, SweetBerryBushBlock.class, StemBlock.class})
public abstract class BlockGrowMixin {
    @WrapOperation(method = "randomTick", at = @At(value = "INVOKE", ordinal = 0,
            target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean lunararc$grow(ServerLevel level, BlockPos pos, BlockState state, int flags, Operation<Boolean> original) {
        return CraftEventFactory.handleBlockGrowEvent(level, pos, state, flags, (p, s, f) -> original.call(level, p, s, f));
    }
}
