package org.bukkit.craftbukkit.v1_21_R1.inventory;

/**
 * Legacy CraftBukkit binary name retained for plugins (notably NBT-API) that
 * reflect CraftMetaItem directly. LunarArc's concrete implementation lives in
 * CraftItemMeta; this class intentionally preserves the expected inheritance
 * surface without duplicating metadata state.
 */
public class CraftMetaItem extends CraftItemMeta {
    public CraftMetaItem() { super(); }
    public CraftMetaItem(net.minecraft.world.item.ItemStack stack) { super(stack); }
}
