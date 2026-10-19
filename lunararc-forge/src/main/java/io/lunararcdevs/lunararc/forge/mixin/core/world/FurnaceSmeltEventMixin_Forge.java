package io.lunararcdevs.lunararc.forge.mixin.core.world;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.lunararcdevs.lunararc.common.event.LunarArcFurnaceState;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** FurnaceSmeltEvent on Forge, whose patched {@code burn} is an instance method (the common variant targets the static one). */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class FurnaceSmeltEventMixin_Forge {
    @WrapOperation(method = "serverTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;burn(Lnet/minecraft/core/NonNullList;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)V"))
    private static void lunararc$furnaceSmelt(AbstractFurnaceBlockEntity furnace, NonNullList<ItemStack> slots, ItemStack source, ItemStack result,
            Operation<Void> original) {
        ItemStack output = LunarArcFurnaceState.smeltResult(slots, source, result);
        if (output != null) original.call(furnace, slots, source, output);
    }
}
