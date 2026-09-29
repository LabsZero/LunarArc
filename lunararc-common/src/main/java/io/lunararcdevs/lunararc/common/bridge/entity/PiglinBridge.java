package io.lunararcdevs.lunararc.common.bridge.entity;

import net.minecraft.world.item.Item;

import java.util.Set;

public interface PiglinBridge {
    Set<Item> lunararc$getAllowedBarterItems();
    Set<Item> lunararc$getInterestItems();
}
