package io.lunararcdevs.lunararc.common.bridge.access;

import net.minecraft.world.item.ItemStackTemplate;

/** Normal runtime bridge implemented by the corresponding Mixin accessor/invoker. */
public interface SmithingTransformRecipeAccessBridge {
    ItemStackTemplate lunararc$result();
}
