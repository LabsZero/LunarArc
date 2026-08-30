package io.ampznetwork.lunararc.common.bridge.access;

import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SmithingRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import javax.annotation.Nullable;

/** Normal runtime bridge implemented by the corresponding Mixin accessor/invoker. */
public interface SmithingMenuAccessBridge {
    @Nullable RecipeHolder<SmithingRecipe> lunararc$getSelectedRecipe();
}
