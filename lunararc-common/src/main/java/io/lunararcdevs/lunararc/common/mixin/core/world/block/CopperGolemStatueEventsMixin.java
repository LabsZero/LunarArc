package io.lunararcdevs.lunararc.common.mixin.core.world.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CopperGolemStatueBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Posing a copper golem statue is an EntityChangeBlockEvent by the interacting player. */
@Mixin(CopperGolemStatueBlock.class)
public abstract class CopperGolemStatueEventsMixin {
    @WrapOperation(method = "updatePose", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean lunararc$pose(Level level, BlockPos pos, BlockState newState, Operation<Boolean> original,
            Level levelArg, BlockState state, BlockPos posArg, Player player) {
        if (!CraftEventFactory.callEntityChangeBlockEvent(player, pos, newState)) return false;
        return original.call(level, pos, newState);
    }
}
