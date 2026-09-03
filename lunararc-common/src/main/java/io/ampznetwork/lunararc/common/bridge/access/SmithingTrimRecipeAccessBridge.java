package io.ampznetwork.lunararc.common.bridge.access;

import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SmithingTrimRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Normal runtime bridge implemented by the corresponding Mixin accessor/invoker. */
public interface SmithingTrimRecipeAccessBridge {
    Ingredient lunararc$template();
    Ingredient lunararc$base();
    Ingredient lunararc$addition();
}
