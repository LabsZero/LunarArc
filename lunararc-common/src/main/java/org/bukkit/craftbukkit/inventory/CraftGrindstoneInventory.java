package org.bukkit.craftbukkit.inventory;

import net.minecraft.world.inventory.GrindstoneMenu;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.GrindstoneInventory;

public final class CraftGrindstoneInventory extends CraftInventoryView.MenuSlotInventory implements GrindstoneInventory {
    public CraftGrindstoneInventory(GrindstoneMenu handle, HumanEntity owner) {
        super(handle, 3, InventoryType.GRINDSTONE, (org.bukkit.inventory.InventoryHolder) owner);
    }
}
