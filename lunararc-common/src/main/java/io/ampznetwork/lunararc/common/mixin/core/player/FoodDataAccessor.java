package io.ampznetwork.lunararc.common.mixin.core.player;

import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FoodData.class)
public interface FoodDataAccessor {
    @Accessor("foodLevel")
    void setFoodLevel(int value);

    @Accessor("saturationLevel")
    void setSaturationLevel(float value);

    @Accessor("exhaustionLevel")
    float getExhaustionLevel();

    @Accessor("exhaustionLevel")
    void setExhaustionLevel(float value);
}
