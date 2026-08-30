package io.ampznetwork.lunararc.common.bridge.access;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Normal runtime bridge implemented by the corresponding Mixin accessor/invoker. */
public interface ShapelessRecipeAccessBridge {
    String lunararc$group();
    CraftingBookCategory lunararc$category();
    ItemStack lunararc$result();
    NonNullList<Ingredient> lunararc$ingredients();
}
