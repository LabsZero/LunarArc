package io.ampznetwork.lunararc.common.mixin.core.world.block.piston;

import com.llamalad7.mixinextras.sugar.Local;
import io.ampznetwork.lunararc.common.mod.util.LunarArcLogicWorlds;
import io.ampznetwork.lunararc.common.mod.util.LunarArcPistonAffectedBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.event.block.BlockPistonEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Fires {@link BlockPistonExtendEvent} and {@link BlockPistonRetractEvent}.
 *
 * <p>This is how every land-protection plugin stops piston grief. Pistons are the standard way
 * around a claim border: the piston sits outside, the blocks it moves are inside, and no
 * block-break or block-place event is involved at any point. WorldGuard, GriefPrevention, Towny,
 * Lands and Residence all rely on these two events for it. Neither was fired here, so a claim that
 * refused a pickaxe could still be taken apart from a block away.</p>
 *
 * <p>Hooked where CraftBukkit hooks it: inside {@code moveBlocks}, after the structure resolver has
 * worked out what moves and what breaks, and before anything is changed. That is what lets the
 * event carry the affected blocks and lets a cancel be a true no-op.</p>
 *
 * <p>Cancelling re-sends the affected positions to clients rather than simply returning false. A
 * client predicts piston movement locally, so by the time the server declines it the player has
 * already seen the blocks move; without the updates they stay wrong on screen until something else
 * touches them. The three sends per moved block - the block itself and the position ahead of it -
 * are CraftBukkit's, and cover both ends of the movement that was predicted.</p>
 *
 * <p>Paper additionally fires an empty-list retract event for a piston pulling nothing back, from
 * two points inside {@code triggerEvent}. That is not ported here: placing it needs an exact
 * instruction ordinal in a method both NeoForge and Forge patch, and it is a refinement to an event
 * that this class is introducing rather than part of making it work.</p>
 */
@Mixin(PistonBaseBlock.class)
public abstract class PistonBaseBlockMixin {

    @Inject(
            method = "moveBlocks",
            at = @At(value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/world/level/block/piston/PistonStructureResolver;getToDestroy()Ljava/util/List;"),
            cancellable = true,
            require = 0)
    private void lunararc$pistonMoveEvent(Level level, BlockPos pos, Direction facing, boolean extending,
            CallbackInfoReturnable<Boolean> cir, @Local PistonStructureResolver resolver) {
        // Guarded by the logic-world test rather than instanceof ServerLevel: mods run block
        // mechanics against simulated and scratch levels that are ServerLevel subclasses, and a
        // Bukkit event naming blocks in a world no plugin has seen is noise at best.
        if (!(level instanceof ServerLevel serverLevel) || !LunarArcLogicWorlds.isLogicWorld(serverLevel)) {
            return;
        }

        org.bukkit.block.Block piston = CraftBlock.at(serverLevel, pos);
        List<BlockPos> moved = resolver.getToPush();
        List<BlockPos> broken = resolver.getToDestroy();
        Direction direction = extending ? facing : facing.getOpposite();

        BlockPistonEvent event = extending
                ? new BlockPistonExtendEvent(piston,
                        new LunarArcPistonAffectedBlocks(piston.getWorld(), moved, broken),
                        CraftBlock.notchToBlockFace(direction))
                : new BlockPistonRetractEvent(piston,
                        new LunarArcPistonAffectedBlocks(piston.getWorld(), moved, broken),
                        CraftBlock.notchToBlockFace(direction));
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) return;

        for (BlockPos position : broken) {
            serverLevel.sendBlockUpdated(position, Blocks.AIR.defaultBlockState(),
                    serverLevel.getBlockState(position), 3);
        }
        for (BlockPos position : moved) {
            serverLevel.sendBlockUpdated(position, Blocks.AIR.defaultBlockState(),
                    serverLevel.getBlockState(position), 3);
            BlockPos ahead = position.relative(direction);
            serverLevel.sendBlockUpdated(ahead, Blocks.AIR.defaultBlockState(),
                    serverLevel.getBlockState(ahead), 3);
        }
        cir.setReturnValue(false);
    }
}
