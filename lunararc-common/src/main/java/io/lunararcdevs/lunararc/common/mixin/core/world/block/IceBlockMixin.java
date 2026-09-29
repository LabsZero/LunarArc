package io.lunararcdevs.lunararc.common.mixin.core.world.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IceBlock.class)
public abstract class IceBlockMixin {
    @Inject(method = "melt", at = @At("HEAD"), cancellable = true)
    private void lunararc$melt(BlockState state, Level level, BlockPos pos, CallbackInfo ci) {
        BlockState melted = level.dimensionType().ultraWarm() ? Blocks.AIR.defaultBlockState() : Blocks.WATER.defaultBlockState();
        if (CraftEventFactory.callBlockFadeCancelled(level, pos, melted)) ci.cancel();
    }
}
