package io.ampznetwork.lunararc.common.mixin.core.world.material;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.FlowingFluid;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.event.block.BlockFromToEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fires {@link BlockFromToEvent} for flowing liquid.
 *
 * <p>This is how land-protection plugins stop lava and water crossing into a claim. Without it a
 * claim that refuses a pickaxe and now refuses TNT could still be flooded or burned out by a bucket
 * poured outside its border, which is the oldest griefing technique there is.</p>
 *
 * <p>CraftBukkit patches the two places {@code FlowingFluid} spreads. Both are reproduced here with
 * the same cancellation semantics, which differ between them and are the reason this needs two
 * different injectors rather than one:</p>
 * <ul>
 *   <li>Spreading down, CraftBukkit returns from {@code spread} outright, so a cancelled flow also
 *       skips the sideways spread that vanilla would do next. An {@code @Inject} that cancels the
 *       callback returns from the method in exactly the same way.</li>
 *   <li>Spreading sideways, CraftBukkit continues the loop, so cancelling one direction leaves the
 *       other three to be considered on their own merits. Only the single {@code spreadTo} call is
 *       suppressed, which is what wrapping that one operation does; cancelling the enclosing method
 *       instead would silently stop checking the remaining directions.</li>
 * </ul>
 *
 * <p>The sideways handler recovers the source position from the arguments it already has -
 * {@code target.relative(direction.getOpposite())} - rather than capturing the enclosing method's
 * parameters, because vanilla reaches the target as {@code source.relative(direction)} and the two
 * are exact inverses.</p>
 */
@Mixin(FlowingFluid.class)
public abstract class FlowingFluidMixin {

    @Inject(
            method = "spread",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FlowingFluid;spreadTo(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;Lnet/minecraft/world/level/material/FluidState;)V"),
            cancellable = true)
    private void lunararc$blockFromToDown(Level level, BlockPos pos, FluidState fluidState, CallbackInfo ci) {
        if (lunararc$flowCancelled(level, pos, Direction.DOWN)) {
            ci.cancel();
        }
    }

    @WrapOperation(
            method = "spreadToSides",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FlowingFluid;spreadTo(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;Lnet/minecraft/world/level/material/FluidState;)V"))
    private void lunararc$blockFromToSides(
            FlowingFluid self,
            LevelAccessor accessor,
            BlockPos target,
            BlockState targetState,
            Direction direction,
            FluidState fluidState,
            Operation<Void> original) {
        BlockPos source = target.relative(direction.getOpposite());
        if (accessor instanceof Level level && lunararc$flowCancelled(level, source, direction)) {
            return;
        }
        original.call(self, accessor, target, targetState, direction, fluidState);
    }

    @Unique
    private static boolean lunararc$flowCancelled(Level level, BlockPos source, Direction direction) {
        if (!(level instanceof ServerLevel serverLevel)) return false;
        BlockFromToEvent event = new BlockFromToEvent(
                CraftBlock.at(serverLevel, source), CraftBlock.notchToBlockFace(direction));
        Bukkit.getPluginManager().callEvent(event);
        return event.isCancelled();
    }
}
