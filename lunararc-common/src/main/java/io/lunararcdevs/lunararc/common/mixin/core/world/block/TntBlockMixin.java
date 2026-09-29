package io.lunararcdevs.lunararc.common.mixin.core.world.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.block.TNTPrimeEvent.PrimeCause;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TntBlock.class)
public abstract class TntBlockMixin {
    @Unique private static final String EXPLODE = "Lnet/minecraft/world/level/block/TntBlock;explode(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V";
    @Unique private static final String EXPLODE_BY = "Lnet/minecraft/world/level/block/TntBlock;explode(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/LivingEntity;)V";
    @Unique private static final String CAUGHT_FIRE = "Lnet/minecraft/world/level/block/TntBlock;onCaughtFire(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/world/entity/LivingEntity;)V";

    @Inject(method = {"onPlace", "neighborChanged"}, cancellable = true, at = {
            @At(value = "INVOKE", target = EXPLODE), @At(value = "INVOKE", target = CAUGHT_FIRE, remap = false)})
    private void lunararc$primeByRedstone(CallbackInfo ci, @Local(argsOnly = true) Level level, @Local(argsOnly = true, ordinal = 0) BlockPos pos) {
        if (!CraftEventFactory.callTNTPrimeEvent(level, pos, PrimeCause.REDSTONE, null, null)) ci.cancel();
    }

    @Inject(method = "onProjectileHit", cancellable = true, at = {
            @At(value = "INVOKE", target = EXPLODE_BY), @At(value = "INVOKE", target = CAUGHT_FIRE, remap = false)})
    private void lunararc$primeByProjectile(Level level, BlockState state, BlockHitResult hit, Projectile projectile, CallbackInfo ci) {
        BlockPos pos = hit.getBlockPos();
        if (!CraftEventFactory.callEntityChangeBlockEvent(projectile, pos, Blocks.AIR.defaultBlockState())
                || !CraftEventFactory.callTNTPrimeEvent(level, pos, PrimeCause.PROJECTILE, projectile, null)) {
            ci.cancel();
        }
    }

    @Inject(method = "useItemOn", cancellable = true, at = {
            @At(value = "INVOKE", target = EXPLODE_BY), @At(value = "INVOKE", target = CAUGHT_FIRE, remap = false)})
    private void lunararc$primeByPlayer(CallbackInfoReturnable<ItemInteractionResult> cir,
            @Local(argsOnly = true) Level level, @Local(argsOnly = true) BlockPos pos, @Local(argsOnly = true) Player player) {
        if (!CraftEventFactory.callTNTPrimeEvent(level, pos, PrimeCause.PLAYER, player, null)) cir.setReturnValue(ItemInteractionResult.CONSUME);
    }

    @WrapOperation(method = "playerWillDestroy", require = 0, at = @At(value = "INVOKE", target = EXPLODE))
    private void lunararc$primeByBreak(Level level, BlockPos pos, Operation<Void> original, Level unused, BlockPos unusedPos,
            BlockState state, Player player) {
        if (CraftEventFactory.callTNTPrimeEvent(level, pos, PrimeCause.BLOCK_BREAK, player, null)) original.call(level, pos);
    }

    @WrapOperation(method = "playerWillDestroy", require = 0, at = @At(value = "INVOKE", target = CAUGHT_FIRE, remap = false))
    private void lunararc$primeByBreakNeo(TntBlock block, BlockState state, Level level, BlockPos pos, Direction face, LivingEntity igniter,
            Operation<Void> original, Level unused, BlockPos unusedPos, BlockState unusedState, Player player) {
        if (CraftEventFactory.callTNTPrimeEvent(level, pos, PrimeCause.BLOCK_BREAK, player, null)) original.call(block, state, level, pos, face, igniter);
    }
}
