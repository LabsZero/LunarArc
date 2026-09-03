package io.ampznetwork.lunararc.common.mixin.core.world;

import io.ampznetwork.lunararc.common.bridge.PlayerExhaustionBridge;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds Bukkit's BLOCK_MINED exhaustion cause at the real Block#playerDestroy call site. */
@Mixin(Block.class)
public abstract class BlockMixin {
    @Inject(method = "playerDestroy", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;causeFoodExhaustion(F)V"), require = 0)
    private void lunararc$blockMinedExhaustion(net.minecraft.world.level.Level level, Player player,
            net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.world.level.block.entity.BlockEntity blockEntity, net.minecraft.world.item.ItemStack tool,
            CallbackInfo ci) {
        ((PlayerExhaustionBridge) player).lunararc$pushExhaustionReason(
                org.bukkit.event.entity.EntityExhaustionEvent.ExhaustionReason.BLOCK_MINED);
    }

    /**
     * Paper's {@code Block#getExpDrop}, which plugins call to ask what a block would drop.
     *
     * <p>Veinminer calls it once per block it breaks and died on NoSuchMethodError, taking the whole
     * scheduled task with it. Paper declares it on Block returning zero and overrides it on the
     * blocks that actually drop experience; this is the base declaration, so the call resolves and
     * the answer is the same one Paper's base class gives.</p>
     *
     * <p>The per-block overrides - DropExperienceBlock and friends - are not here yet, so ores
     * report no experience through this API rather than their real amount. That is a wrong answer
     * where Paper has a right one, and it is worth saying plainly: it is the difference between a
     * plugin getting zero and a plugin crashing, not the end of the work.</p>
     */
    @org.spongepowered.asm.mixin.Unique
    public int getExpDrop(net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos,
            net.minecraft.world.item.ItemStack tool, boolean dropExperience) {
        return 0;
    }
}
