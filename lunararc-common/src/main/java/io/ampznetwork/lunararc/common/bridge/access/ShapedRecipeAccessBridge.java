package io.ampznetwork.lunararc.common.bridge.access;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Normal runtime bridge implemented by the corresponding Mixin accessor/invoker. */
public interface ShapedRecipeAccessBridge {
    ShapedRecipePattern lunararc$pattern();
    ItemStack lunararc$result();
    String lunararc$group();
    CraftingBookCategory lunararc$category();
}
