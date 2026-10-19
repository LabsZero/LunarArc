package io.lunararcdevs.lunararc.common.mixin.core.recipe;

import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read-only CraftBukkit view of vanilla 26.x smithing-transform state. */
@Mixin(SmithingTransformRecipe.class)
public interface SmithingTransformRecipeAccessor extends io.lunararcdevs.lunararc.common.bridge.access.SmithingTransformRecipeAccessBridge {
    @Accessor("result") ItemStackTemplate lunararc$result();
}
