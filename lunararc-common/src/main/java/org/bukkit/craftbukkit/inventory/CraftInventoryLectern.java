package org.bukkit.craftbukkit.inventory;

import io.lunararcdevs.lunararc.common.bridge.donor.DonorContainerSupport;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import org.bukkit.block.Lectern;
import org.bukkit.inventory.LecternInventory;

public class CraftInventoryLectern extends CraftInventory implements LecternInventory {
    public MenuProvider tile;

    public CraftInventoryLectern(Container inventory) {
        super(inventory);
        this.tile = DonorContainerSupport.lecternOf(inventory);
    }

    @Override
    public Lectern getHolder() {
        if (this.tile instanceof LecternBlockEntity lectern && lectern.getLevel() instanceof ServerLevel level) {
            return (Lectern) org.bukkit.craftbukkit.block.CraftBlock.at(level, lectern.getBlockPos()).getState();
        }
        return null;
    }
}
