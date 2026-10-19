package io.lunararcdevs.lunararc.common.mixin.core.recipe;

import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read-only CraftBukkit view of vanilla 26.x single-item recipe state (input/group are public). */
@Mixin(SingleItemRecipe.class)
public interface SingleItemRecipeAccessor extends io.lunararcdevs.lunararc.common.bridge.access.SingleItemRecipeAccessBridge {
    @Accessor("result") ItemStackTemplate lunararc$result();
}
