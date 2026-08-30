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
}
