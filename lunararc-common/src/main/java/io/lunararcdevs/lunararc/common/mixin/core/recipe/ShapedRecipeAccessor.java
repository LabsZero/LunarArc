package io.lunararcdevs.lunararc.common.mixin.core.recipe;

import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read-only CraftBukkit view of vanilla 26.x shaped-recipe state (group/category are public on the recipe). */
@Mixin(ShapedRecipe.class)
public interface ShapedRecipeAccessor extends io.lunararcdevs.lunararc.common.bridge.access.ShapedRecipeAccessBridge {
    @Accessor("pattern") ShapedRecipePattern lunararc$pattern();
    @Accessor("result") ItemStackTemplate lunararc$result();
}
