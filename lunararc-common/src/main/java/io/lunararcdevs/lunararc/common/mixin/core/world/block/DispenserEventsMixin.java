package io.lunararcdevs.lunararc.common.mixin.core.world.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.lunararcdevs.lunararc.common.event.LunarArcMoreEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DispenserBlock.class)
public abstract class DispenserEventsMixin {

    @WrapOperation(method = "dispenseFrom", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/DispenserBlockEntity;getRandomSlot(Lnet/minecraft/util/RandomSource;)I"))
    private int lunararc$failed(DispenserBlockEntity dispenser, RandomSource random, Operation<Integer> original,
            @Local(argsOnly = true) ServerLevel level, @Local(argsOnly = true) BlockPos pos) {
        int slot = original.call(dispenser, random);
        if (slot < 0 && !LunarArcMoreEvents.dispenseFailed(level, pos)) return Integer.MAX_VALUE;
        return slot;
    }

    @WrapOperation(method = "dispenseFrom", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/dispenser/DispenseItemBehavior;dispense(Lnet/minecraft/core/dispenser/BlockSource;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack lunararc$pre(DispenseItemBehavior behavior, BlockSource source, ItemStack stack, Operation<ItemStack> original,
            @Local(argsOnly = true) ServerLevel level, @Local(argsOnly = true) BlockPos pos) {
        return LunarArcMoreEvents.dispenseAllowed(level, pos, stack) ? original.call(behavior, source, stack) : stack;
    }
}
