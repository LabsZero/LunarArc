package io.lunararcdevs.lunararc.common.mixin.core.world.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.lunararcdevs.lunararc.common.compat.LunarArcHarvestCapture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SweetBerryBushBlock.class)
public abstract class SweetBerryBushBlockMixin {
    @WrapOperation(method = "useWithoutItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/SweetBerryBushBlock;popResource(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)V"))
    private void lunararc$harvest(Level level, BlockPos pos, ItemStack drop, Operation<Void> original,
            BlockState state, Level unused, BlockPos unusedPos, Player player, BlockHitResult hit) {
        LunarArcHarvestCapture.harvest(level, pos, player, drop, item -> original.call(level, pos, item));
    }

    @Inject(method = "useWithoutItem", cancellable = true, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V"))
    private void lunararc$harvestCancelled(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit,
            CallbackInfoReturnable<InteractionResult> cir) {
        if (LunarArcHarvestCapture.consumeCancelled()) cir.setReturnValue(InteractionResult.SUCCESS);
    }
}
