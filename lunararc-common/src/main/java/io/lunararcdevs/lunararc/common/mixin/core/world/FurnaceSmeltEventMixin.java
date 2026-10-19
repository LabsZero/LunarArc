package io.lunararcdevs.lunararc.common.mixin.core.world;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.lunararcdevs.lunararc.common.event.LunarArcFurnaceState;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** FurnaceSmeltEvent where {@code burn} is a static helper (vanilla, Fabric/Quilt, NeoForge). Forge has its own instance variant. */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class FurnaceSmeltEventMixin {
    @WrapOperation(method = "serverTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;burn(Lnet/minecraft/core/NonNullList;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)V"))
    private static void lunararc$furnaceSmelt(NonNullList<ItemStack> slots, ItemStack source, ItemStack result, Operation<Void> original) {
        ItemStack output = LunarArcFurnaceState.smeltResult(slots, source, result);
        if (output != null) original.call(slots, source, output);
    }
}
