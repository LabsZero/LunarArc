package org.bukkit.craftbukkit.inventory;


@org.bukkit.configuration.serialization.DelegateDeserialization(SerializableMeta.class)
public class CraftMetaItem extends CraftItemMeta {
    public CraftMetaItem() { super(); }
    public CraftMetaItem(net.minecraft.world.item.ItemStack stack) { super(stack); }
}
