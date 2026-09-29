package io.lunararcdevs.lunararc.common.mixin.core.world.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LeavesBlock.class)
public abstract class LeavesBlockMixin {
    @Inject(method = "randomTick", cancellable = true, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/LeavesBlock;dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"))
    private void lunararc$decay(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (!org.bukkit.Bukkit.isPrimaryThread()) return;
        var event = new org.bukkit.event.block.LeavesDecayEvent(CraftBlock.at(level, pos));
        org.bukkit.Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled() || level.getBlockState(pos).getBlock() != (Object) this) ci.cancel();
    }
}
