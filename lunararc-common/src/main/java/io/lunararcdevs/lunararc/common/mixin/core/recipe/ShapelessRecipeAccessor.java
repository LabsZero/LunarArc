package io.lunararcdevs.lunararc.common.mixin.core.recipe;

import java.util.List;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read-only CraftBukkit view of vanilla 26.x shapeless-recipe state (group/category are public on the recipe). */
@Mixin(ShapelessRecipe.class)
public interface ShapelessRecipeAccessor extends io.lunararcdevs.lunararc.common.bridge.access.ShapelessRecipeAccessBridge {
    @Accessor("result") ItemStackTemplate lunararc$result();
    @Accessor("ingredients") List<Ingredient> lunararc$ingredients();
}
