package io.lunararcdevs.lunararc.common.mixin.core.recipe;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.lunararcdevs.lunararc.common.bridge.recipe.CopyDataComponentsBridge;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SmithingTrimRecipe;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SmithingTrimRecipe.class)
public abstract class SmithingTrimRecipeMixin implements CopyDataComponentsBridge {
    @Unique private boolean lunararc$copyDataComponents = true;

    @Override public boolean lunararc$copyDataComponents() { return this.lunararc$copyDataComponents; }
    @Override public void lunararc$copyDataComponents(boolean copy) { this.lunararc$copyDataComponents = copy; }

    /** Paper's "copyDataComponents=false": apply the trim to a bare copy of the base item type. */
    @WrapOperation(method = "assemble(Lnet/minecraft/world/item/crafting/SmithingRecipeInput;)Lnet/minecraft/world/item/ItemStack;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/crafting/SmithingTrimRecipe;applyTrim(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/Holder;)Lnet/minecraft/world/item/ItemStack;"),
            require = 0)
    private ItemStack lunararc$controlComponentCopy(ItemStack base, ItemStack addition, Holder<TrimPattern> pattern, Operation<ItemStack> original) {
        ItemStack effectiveBase = this.lunararc$copyDataComponents ? base : new ItemStack(base.getItem(), 1);
        return original.call(effectiveBase, addition, pattern);
    }
}
