package io.lunararcdevs.lunararc.common.mixin.core.world;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.lunararcdevs.lunararc.common.event.LunarArcMoreEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(HopperBlockEntity.class)
public abstract class HopperSearchEventsMixin {

    @ModifyReturnValue(method = "getAttachedContainer", at = @At("RETURN"), require = 0)
    private static Container lunararc$destination(Container container, Level level, BlockPos pos, HopperBlockEntity hopper) {
        return LunarArcMoreEvents.hopperSearch(container, level, pos, pos.relative(hopper.getBlockState().getValue(HopperBlock.FACING)), true);
    }

    @ModifyReturnValue(method = "getSourceContainer", at = @At("RETURN"), require = 0)
    private static Container lunararc$source(Container container, Level level, Hopper hopper, BlockPos pos, BlockState state) {
        return LunarArcMoreEvents.hopperSearch(container, level,
                BlockPos.containing(hopper.getLevelX(), hopper.getLevelY(), hopper.getLevelZ()), pos, false);
    }
}
