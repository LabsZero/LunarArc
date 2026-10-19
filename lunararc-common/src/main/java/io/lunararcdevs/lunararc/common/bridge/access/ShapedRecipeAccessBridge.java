package io.lunararcdevs.lunararc.common.bridge.access;

import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

/** Normal runtime bridge implemented by the corresponding Mixin accessor/invoker. */
public interface ShapedRecipeAccessBridge {
    ShapedRecipePattern lunararc$pattern();
    ItemStackTemplate lunararc$result();
}
