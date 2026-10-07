package io.lunararcdevs.lunararc.common.mixin.core.redstone;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.lunararcdevs.lunararc.common.event.LunarArcPaperEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.world.level.block.ButtonBlock;

@Mixin(ButtonBlock.class)
public abstract class ButtonBlockMixin {

    @WrapOperation(method = "useWithoutItem", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/ButtonBlock;press(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;)V"))
    private void lunararc$press(ButtonBlock button, BlockState state, Level level, BlockPos pos, Player player, Operation<Void> original) {
        if (LunarArcPaperEvents.redstoneAllows(level, pos, false, true)) original.call(button, state, level, pos, player);
    }
}
