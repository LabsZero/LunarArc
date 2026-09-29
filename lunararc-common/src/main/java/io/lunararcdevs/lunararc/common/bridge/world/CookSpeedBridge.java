package io.lunararcdevs.lunararc.common.bridge.world;

import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;

public interface CookSpeedBridge {
    double lunararc$cookSpeed();

    RecipeType<? extends AbstractCookingRecipe> lunararc$recipeType();
}
