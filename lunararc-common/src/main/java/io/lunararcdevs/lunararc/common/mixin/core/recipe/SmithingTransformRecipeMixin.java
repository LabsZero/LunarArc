package io.lunararcdevs.lunararc.common.mixin.core.recipe;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.lunararcdevs.lunararc.common.bridge.recipe.CopyDataComponentsBridge;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.item.crafting.TransmuteRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SmithingTransformRecipe.class)
public abstract class SmithingTransformRecipeMixin implements CopyDataComponentsBridge {
    @Unique private boolean lunararc$copyDataComponents = true;
    @Override public boolean lunararc$copyDataComponents() { return this.lunararc$copyDataComponents; }
    @Override public void lunararc$copyDataComponents(boolean copy) { this.lunararc$copyDataComponents = copy; }

    /** Paper's "copyDataComponents=false": the result is built only from the recipe result, not the base item. */
    @WrapOperation(method = "assemble(Lnet/minecraft/world/item/crafting/SmithingRecipeInput;)Lnet/minecraft/world/item/ItemStack;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/crafting/TransmuteRecipe;createWithOriginalComponents(Lnet/minecraft/world/item/ItemStackTemplate;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"),
            require = 0)
    private ItemStack lunararc$assembleWithoutResultComponents(ItemStackTemplate result, ItemStack base, Operation<ItemStack> original) {
        return this.lunararc$copyDataComponents ? original.call(result, base) : result.create();
    }
}
