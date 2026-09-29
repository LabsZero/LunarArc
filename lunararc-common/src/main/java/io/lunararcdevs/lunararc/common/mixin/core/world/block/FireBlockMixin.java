package io.lunararcdevs.lunararc.common.mixin.core.world.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FireBlock.class)
public abstract class FireBlockMixin {

    @Shadow protected abstract BlockState getStateWithAge(LevelAccessor level, BlockPos pos, int age);

    @Unique
    private BlockPos lunararc$tickingFire;

    @Inject(method = "tick", at = @At("HEAD"))
    private void lunararc$recordTickingFire(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        this.lunararc$tickingFire = pos;
    }

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
        if (source == null || target.equals(source)) {
            return original.call(level, target, newState, flags);
        }
        if (CraftEventFactory.callBlockIgniteEvent(level, target, source).isCancelled()) {
            return false;
        }
        return CraftEventFactory.handleBlockSpreadEvent(level, source, target, newState, flags,
                (p, s, f) -> original.call(level, p, s, f));
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"))
    private boolean lunararc$fireExtinguished(ServerLevel level, BlockPos pos, boolean moving, Operation<Boolean> original) {
        if (CraftEventFactory.callBlockFadeCancelled(level, pos, Blocks.AIR.defaultBlockState())) return false;
        return original.call(level, pos, moving);
    }

    @Inject(method = "checkBurnOut", cancellable = true,
            at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/util/RandomSource;nextInt(I)I"))
    private void lunararc$blockBurn(CallbackInfo ci, @Local(argsOnly = true) Level level, @Local(argsOnly = true) BlockPos pos) {
        BlockPos source = this.lunararc$tickingFire;
        if (!(level instanceof ServerLevel serverLevel) || source == null || !org.bukkit.Bukkit.isPrimaryThread()) return;
        var event = new org.bukkit.event.block.BlockBurnEvent(CraftBlock.at(serverLevel, pos), CraftBlock.at(serverLevel, source));
        org.bukkit.Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            ci.cancel();
            return;
        }
        if (level.getBlockState(pos).getBlock() instanceof TntBlock
                && !CraftEventFactory.callTNTPrimeEvent(level, pos, org.bukkit.event.block.TNTPrimeEvent.PrimeCause.FIRE, null, source)) {
            ci.cancel();
        }
    }

    @Inject(method = "updateShape", at = @At("RETURN"), cancellable = true)
    private void lunararc$fadeOnUpdate(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
            BlockPos pos, BlockPos neighborPos, CallbackInfoReturnable<BlockState> cir) {
        if (!cir.getReturnValue().isAir() || !(level instanceof ServerLevel)) return;
        if (CraftEventFactory.callBlockFadeCancelled(level, pos, Blocks.AIR.defaultBlockState())) {
            cir.setReturnValue(this.getStateWithAge(level, pos, state.getValue(FireBlock.AGE)));
        }
    }
}
