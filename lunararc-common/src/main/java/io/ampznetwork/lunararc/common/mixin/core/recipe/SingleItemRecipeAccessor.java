package io.ampznetwork.lunararc.common.mixin.core.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read-only CraftBukkit view of vanilla 1.21.1 single-item recipe state. */
@Mixin(SingleItemRecipe.class)
public interface SingleItemRecipeAccessor extends io.ampznetwork.lunararc.common.bridge.access.SingleItemRecipeAccessBridge {
    @Accessor("group") String lunararc$group();
    @Accessor("ingredient") Ingredient lunararc$ingredient();
    @Accessor("result") ItemStack lunararc$result();
}
