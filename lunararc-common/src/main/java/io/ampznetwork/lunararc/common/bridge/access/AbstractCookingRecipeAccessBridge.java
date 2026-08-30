package io.ampznetwork.lunararc.common.bridge.access;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Normal runtime bridge implemented by the corresponding Mixin accessor/invoker. */
public interface AbstractCookingRecipeAccessBridge {
    CookingBookCategory lunararc$category();
    String lunararc$group();
    Ingredient lunararc$ingredient();
    ItemStack lunararc$result();
    float lunararc$experience();
    int lunararc$cookingTime();
}
