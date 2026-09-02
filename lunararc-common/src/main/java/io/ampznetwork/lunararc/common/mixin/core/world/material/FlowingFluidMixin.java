package io.ampznetwork.lunararc.common.mixin.core.world.material;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
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
 *       other three to be considered on their own merits. Answering "cannot spread" for the single
 *       direction under consideration does that; cancelling the enclosing method instead would
 *       silently stop checking the remaining directions.</li>
 * </ul>
 *
 * <p>Both handlers only fire against a real logic world; see LunarArcLogicWorlds for why
 * {@code instanceof ServerLevel} is the wrong test on a modded server.</p>
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

    /**
     * Sideways spread, hooked where Arclight hooks it.
     *
     * <p>This used to wrap {@code spreadTo}, the action, and recover the source position by
     * inverting the direction from the target. Arclight redirects {@code canSpreadTo}, the
     * decision, which hands it the real {@code fromPos} instead of one inferred from an assumed
     * inverse. Taking the value vanilla already computed removes a standing assumption, and asking
     * the question at the decision point is also where a "no" belongs.</p>
     */
    @WrapOperation(
            method = "spreadToSides",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FlowingFluid;canSpreadTo(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/FluidState;Lnet/minecraft/world/level/material/Fluid;)Z"),
            require = 0)
    private boolean lunararc$blockFromToSides(
            FlowingFluid self,
            net.minecraft.world.level.BlockGetter reader,
            BlockPos fromPos,
            BlockState fromState,
            Direction direction,
            BlockPos toPos,
            BlockState toState,
            FluidState toFluid,
            net.minecraft.world.level.material.Fluid fluid,
            Operation<Boolean> original) {
        if (!original.call(self, reader, fromPos, fromState, direction, toPos, toState, toFluid, fluid)) {
            return false;
        }
        return !(reader instanceof Level level) || !lunararc$flowCancelled(level, fromPos, direction);
    }

    /**
     * Whether a plugin cancelled this flow.
     *
     * <p>Guarded by the logic-world test rather than {@code instanceof ServerLevel}. Mods run fluid
     * ticks against simulated and scratch levels that are ServerLevel subclasses, and firing a
     * Bukkit event naming a block in a world no plugin has seen is at best noise - at worst it
     * throws from inside a vanilla mechanic that is mid-tick, and the mechanic stops.</p>
     */
    @Unique
    private static boolean lunararc$flowCancelled(Level level, BlockPos source, Direction direction) {
        if (!(level instanceof ServerLevel serverLevel)
                || !io.ampznetwork.lunararc.common.mod.util.LunarArcLogicWorlds.isLogicWorld(serverLevel)) {
            return false;
        }
        BlockFromToEvent event = new BlockFromToEvent(
                CraftBlock.at(serverLevel, source), CraftBlock.notchToBlockFace(direction));
        Bukkit.getPluginManager().callEvent(event);
        return event.isCancelled();
    }
}
