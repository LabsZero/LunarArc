package io.ampznetwork.lunararc.common.bridge.access;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Normal runtime bridge implemented by the corresponding Mixin accessor/invoker. */
public interface SingleItemRecipeAccessBridge {
    String lunararc$group();
    Ingredient lunararc$ingredient();
    ItemStack lunararc$result();
}
