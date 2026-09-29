package io.lunararcdevs.lunararc.common.mixin.core.world.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.lunararcdevs.lunararc.common.compat.LunarArcHarvestCapture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CaveVines.class)
public interface CaveVinesMixin {
    @WrapOperation(method = "use", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/Block;popResource(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)V"))
    private static void lunararc$harvest(Level level, BlockPos pos, ItemStack drop, Operation<Void> original,
            Entity entity, BlockState state, Level unused, BlockPos unusedPos) {
        if (entity != null && !CraftEventFactory.callEntityChangeBlockEvent(entity, pos, state.setValue(CaveVines.BERRIES, false))) {
            LunarArcHarvestCapture.cancel();
            return;
        }
        LunarArcHarvestCapture.harvest(level, pos, entity instanceof Player player ? player : null, drop, item -> original.call(level, pos, item));
    }

    @Inject(method = "use", cancellable = true, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V"))
    private static void lunararc$harvestCancelled(Entity entity, BlockState state, Level level, BlockPos pos,
            CallbackInfoReturnable<InteractionResult> cir) {
        if (LunarArcHarvestCapture.consumeCancelled()) cir.setReturnValue(InteractionResult.SUCCESS);
    }
}
