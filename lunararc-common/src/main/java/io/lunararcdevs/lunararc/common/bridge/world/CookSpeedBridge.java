package io.lunararcdevs.lunararc.common.bridge.world;

import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;

public interface CookSpeedBridge {
    double lunararc$cookSpeed();

    boolean lunararc$isLit();

    int lunararc$cookingProgress();

    int lunararc$cookingTotalTime();

    void lunararc$setCookingTotalTime(int ticks);

    RecipeType<? extends AbstractCookingRecipe> lunararc$recipeType();
}
