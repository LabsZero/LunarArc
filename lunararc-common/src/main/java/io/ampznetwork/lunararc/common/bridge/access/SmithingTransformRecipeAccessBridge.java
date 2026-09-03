package io.ampznetwork.lunararc.common.bridge.access;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Normal runtime bridge implemented by the corresponding Mixin accessor/invoker. */
public interface SmithingTransformRecipeAccessBridge {
    Ingredient lunararc$template();
    Ingredient lunararc$base();
    Ingredient lunararc$addition();
    ItemStack lunararc$result();
}
