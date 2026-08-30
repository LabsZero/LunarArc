package org.bukkit.craftbukkit.inventory;

/**
 * Minimal CraftBukkit merchant contract required by Paper-patched NMS Merchant.
 * Concrete implementations remain in LunarArc's versioned Craft package.
 */
public interface CraftMerchant extends org.bukkit.inventory.Merchant {
    net.minecraft.world.item.trading.Merchant getMerchant();
}
