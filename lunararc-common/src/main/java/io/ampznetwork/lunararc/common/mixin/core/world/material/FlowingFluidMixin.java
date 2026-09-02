package io.ampznetwork.lunararc.common.mixin.core.world.material;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.ampznetwork.lunararc.common.LunarArcDebug;
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
 *
 * <p>The HEAD hooks on {@code tick} and {@code spread} do nothing at all unless the fluid debug
 * channel is on. They exist because "placed water does not flow" has four possible causes -
 * the placement never scheduling a tick, the scheduled tick never running, vanilla refusing the
 * spread, or this class cancelling it - and no amount of reading tells them apart. Run with
 * {@code -Dlunararc.debug=fluid}, pour a bucket, and the trace in {@code logs/lunararc-debug.log}
 * says which stage the fluid reached.</p>
 */
@Mixin(FlowingFluid.class)
public abstract class FlowingFluidMixin {

    @Inject(method = "tick", at = @At("HEAD"), require = 0)
    private void lunararc$traceTick(Level level, BlockPos pos, FluidState fluidState, CallbackInfo ci) {
        if (!LunarArcDebug.FLUID) return;
        LunarArcDebug.fluid("tick {} at {} amount={} source={} level={}",
                fluidState.getType(), pos, fluidState.getAmount(), fluidState.isSource(),
                level.getClass().getName());
    }

    @Inject(method = "spread", at = @At("HEAD"), require = 0)
    private void lunararc$traceSpread(Level level, BlockPos pos, FluidState fluidState, CallbackInfo ci) {
        if (!LunarArcDebug.FLUID) return;
        LunarArcDebug.fluid("spread {} at {} source={} logicWorld={} listeners={}",
                fluidState.getType(), pos, fluidState.isSource(),
                io.ampznetwork.lunararc.common.mod.util.LunarArcLogicWorlds.isLogicWorld(level),
                BlockFromToEvent.getHandlerList().getRegisteredListeners().length);
    }

    @Inject(
            method = "spread",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FlowingFluid;spreadTo(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;Lnet/minecraft/world/level/material/FluidState;)V"),
            cancellable = true)
    private void lunararc$blockFromToDown(Level level, BlockPos pos, FluidState fluidState, CallbackInfo ci) {
        boolean cancelled = lunararc$flowCancelled(level, pos, Direction.DOWN);
        if (LunarArcDebug.FLUID) {
            LunarArcDebug.fluid("spread down from {} cancelled={}", pos, cancelled);
        }
        if (cancelled) {
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
        boolean vanilla = original.call(self, reader, fromPos, fromState, direction, toPos, toState, toFluid, fluid);
        boolean cancelled = vanilla && reader instanceof Level level && lunararc$flowCancelled(level, fromPos, direction);
        if (LunarArcDebug.FLUID) {
            LunarArcDebug.fluid("spread {} from {} vanillaAllows={} cancelled={}",
                    direction, fromPos, vanilla, cancelled);
        }
        return vanilla && !cancelled;
    }

    /**
     * Whether a plugin cancelled this flow.
     *
     * <p>Guarded by the logic-world test rather than {@code instanceof ServerLevel}. Mods run fluid
     * ticks against simulated and scratch levels that are ServerLevel subclasses, and firing a
     * Bukkit event naming a block in a world no plugin has seen is at best noise - at worst it
     * throws from inside a vanilla mechanic that is mid-tick, and the mechanic stops.</p>
     *
     * <p>Guarded on the handler list first, for the same reason Paper guards its own hot events:
     * every flowing fluid in every loaded chunk arrives here several times a second, and building a
     * CraftBlock and an event object for each one is pure waste on a server where nothing is
     * listening. With no listener there is also no possible verdict but "not cancelled", so the
     * check cannot change behaviour - only what it costs to reach it.</p>
     */
    @Unique
    private static boolean lunararc$flowCancelled(Level level, BlockPos source, Direction direction) {
        if (BlockFromToEvent.getHandlerList().getRegisteredListeners().length == 0) return false;
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
