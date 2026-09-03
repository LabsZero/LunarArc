package org.bukkit.craftbukkit.inventory;

import net.minecraft.world.Container;
import org.bukkit.entity.Horse;
import org.bukkit.inventory.HorseInventory;
import org.bukkit.inventory.ItemStack;

public final class CraftInventoryHorse extends CraftInventoryAbstractHorse implements HorseInventory {
    public CraftInventoryHorse(Container main, Container bodyArmor, Horse owner) { super(main, bodyArmor, owner); }
    @Override public ItemStack getArmor() { return super.getArmor(); }
    @Override public void setArmor(ItemStack stack) { super.setArmor(stack); }
}
