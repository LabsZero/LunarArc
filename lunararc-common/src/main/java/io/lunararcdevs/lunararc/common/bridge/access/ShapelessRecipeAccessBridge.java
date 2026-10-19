package io.lunararcdevs.lunararc.common.bridge.access;

import java.util.List;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;

/** Normal runtime bridge implemented by the corresponding Mixin accessor/invoker. */
public interface ShapelessRecipeAccessBridge {
    ItemStackTemplate lunararc$result();
    List<Ingredient> lunararc$ingredients();
}
