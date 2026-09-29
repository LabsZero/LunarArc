package io.lunararcdevs.lunararc.common.bridge.access;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.DataSlot;

public interface EnchantmentMenuAccessBridge {
    Container lunararc$getEnchantSlots();
    DataSlot lunararc$getEnchantmentSeed();
}
