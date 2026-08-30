package org.bukkit.craftbukkit.inventory;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.bukkit.Location;
import org.bukkit.block.BrewingStand;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Live Bukkit brewer inventory over the loader-owned BrewingStandBlockEntity. */
public final class CraftBrewerInventory extends CraftNMSInventory implements BrewerInventory {
    private final BrewingStandBlockEntity handle;

    public CraftBrewerInventory(BrewingStandBlockEntity handle) {
        super(handle, null, InventoryType.BREWING);
        this.handle = java.util.Objects.requireNonNull(handle, "handle");
    }

    @Override public @Nullable ItemStack getIngredient() { return getItem(3); }
    @Override public void setIngredient(@Nullable ItemStack ingredient) { setItem(3, ingredient); }
    @Override public @Nullable ItemStack getFuel() { return getItem(4); }
    @Override public void setFuel(@Nullable ItemStack fuel) { setItem(4, fuel); }

    @Override
    public @Nullable BrewingStand getHolder() {
        // A dedicated CraftBrewingStand block-state adapter is not required for inventory mutation;
        // Bukkit explicitly permits a null holder for BrewerInventory.
        return null;
    }

    @Override
    public @Nullable BrewingStand getHolder(boolean useSnapshot) {
        return getHolder();
    }

    @Override
    public @Nullable Location getLocation() {
        if (!(handle.getLevel() instanceof ServerLevel level)) return null;
        return org.bukkit.craftbukkit.block.CraftBlock.create(level, handle.getBlockPos()).getLocation();
    }
}
