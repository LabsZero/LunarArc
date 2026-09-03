package io.ampznetwork.lunararc.common.mixin.core.world.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fires {@link org.bukkit.event.block.BlockIgniteEvent} when fire spreads to a new block.
 *
 * <p>Together with the explosion and liquid events this closes the third way a claim gets destroyed
 * from outside its border: light a fire next to it and let the fire tick carry it in. A protection
 * plugin cancels this event to stop fire crossing a boundary, and with the event never fired it had
 * nothing to cancel.</p>
 *
 * <p>Only the spread is hooked here, not the burning away of the block afterwards. That is not an
 * arbitrary split - blocking the spread means fire never arrives to burn anything, so this covers
 * the protection case on its own. CraftBukkit's BlockBurnEvent lives in {@code checkBurnOut}, whose
 * signature CraftBukkit changes to carry the igniting direction, and a mixin cannot change a
 * signature; that one needs its own approach rather than being forced in alongside this.</p>
 *
 * <p>{@code tick} calls {@code setBlock} twice: once on the fire's own position to raise its age,
 * and once on a neighbouring position when it spreads. They are told apart by position rather than
 * by call ordinal, which would break the moment vanilla reorders the method. The fire's own
 * position is recorded at the head of {@code tick}, mirroring the field CraftBukkit adds to
 * FireBlock for the same purpose; block ticking is single-threaded on the server thread, so a plain
 * field is enough to carry it the few instructions it needs to travel.</p>
 */
@Mixin(FireBlock.class)
public abstract class FireBlockMixin {

    @Unique
    private BlockPos lunararc$tickingFire;

    @Inject(method = "tick", at = @At("HEAD"))
    private void lunararc$recordTickingFire(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        this.lunararc$tickingFire = pos;
    }

    // setBlock is declared on Level but invoked here through tick's ServerLevel parameter, and the
    // owner written into a call site is the static type of the receiver - so ServerLevel, matching
    // PortalForcerMixin, which already targets this same method the same way.
    @WrapOperation(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;setBlock("
                            + "Lnet/minecraft/core/BlockPos;"
                            + "Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean lunararc$blockIgniteOnSpread(
            ServerLevel level,
            BlockPos target,
            BlockState newState,
            int flags,
            Operation<Boolean> original) {
        BlockPos source = this.lunararc$tickingFire;
        if (source != null && !target.equals(source)
                && CraftEventFactory.callBlockIgniteEvent(level, target, source).isCancelled()) {
            // Vanilla continues the spread loop after a failed placement, so reporting "not placed"
            // is exactly what cancelling means here.
            return false;
        }
        return original.call(level, target, newState, flags);
    }
}
