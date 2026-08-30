package org.bukkit.craftbukkit.inventory;

import net.minecraft.world.Container;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.inventory.SaddledHorseInventory;

public final class CraftSaddledInventory extends CraftInventoryAbstractHorse implements SaddledHorseInventory {
    public CraftSaddledInventory(Container main, Container bodyArmor, AbstractHorse owner) { super(main, bodyArmor, owner); }
}
