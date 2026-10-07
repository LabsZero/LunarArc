package io.lunararcdevs.lunararc.common.mixin.core.world.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.lunararcdevs.lunararc.common.event.LunarArcMoreEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FlowerPotBlock.class)
public abstract class FlowerPotEventsMixin {

    @WrapOperation(method = "useItemOn", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean lunararc$plant(Level level, BlockPos pos, BlockState state, int flags, Operation<Boolean> original,
            @Local(argsOnly = true) Player player, @Local(argsOnly = true) ItemStack stack) {
        if (LunarArcMoreEvents.flowerPot(player, level, pos, stack)) return original.call(level, pos, state, flags);
        if (!player.hasInfiniteMaterials()) stack.grow(1);
        return false;
    }
}
