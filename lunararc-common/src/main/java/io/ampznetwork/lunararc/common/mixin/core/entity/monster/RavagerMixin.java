package io.ampznetwork.lunararc.common.mixin.core.entity.monster;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.level.Level;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Fires {@link org.bukkit.event.entity.EntityChangeBlockEvent} when a ravager tears through leaves.
 *
 * <p>First of the mob-griefing call sites. Vanilla writes the destruction as
 * {@code flag = this.level().destroyBlock(pos, true, this) || flag}, and CraftBukkit guards it with
 * a {@code continue} that skips the whole assignment. Reporting false from the wrapped call is
 * exactly that: {@code flag} becomes {@code false || flag}, which leaves it unchanged, so a
 * cancelled break neither destroys the block nor makes the ravager think it broke something.</p>
 *
 * <p>The state handed to the event is the block's own fluid state as a legacy block, matching the
 * correction Paper made to CraftBukkit here - it describes what the position becomes once the block
 * is gone, which is what a plugin inspecting the event expects to see.</p>
 */
@Mixin(Ravager.class)
public abstract class RavagerMixin {

    @WrapOperation(
            method = "aiStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;destroyBlock("
                            + "Lnet/minecraft/core/BlockPos;Z"
                            + "Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean lunararc$ravagerBreakLeaves(
            Level level, BlockPos pos, boolean drop, Entity breaker, Operation<Boolean> original) {
        // Read before the block goes, so the event describes the position after the break.
        net.minecraft.world.level.block.state.BlockState after =
                level.getBlockState(pos).getFluidState().createLegacyBlock();
        if (!CraftEventFactory.callEntityChangeBlockEvent((Ravager) (Object) this, pos, after)) {
            return false;
        }
        return original.call(level, pos, drop, breaker);
    }
}
