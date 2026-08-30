package org.bukkit.craftbukkit.inventory;

import org.bukkit.Material;
import org.bukkit.inventory.ItemCraftResult;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Concrete Paper 1.21.1 crafting result container. */
public final class CraftItemCraftResult implements ItemCraftResult {
    private final ItemStack result;
    private final ItemStack[] resultingMatrix = new ItemStack[9];
    private final List<ItemStack> overflowItems = new ArrayList<>();

    public CraftItemCraftResult(ItemStack result) {
        this.result = Objects.requireNonNullElseGet(result, () -> new ItemStack(Material.AIR));
        for (int i = 0; i < this.resultingMatrix.length; i++) {
            this.resultingMatrix[i] = new ItemStack(Material.AIR);
        }
    }

    @Override
    public ItemStack getResult() {
        return this.result;
    }

    @Override
    public ItemStack[] getResultingMatrix() {
        return this.resultingMatrix;
    }

    @Override
    public List<ItemStack> getOverflowItems() {
        return this.overflowItems;
    }

    public void setResultMatrix(int slot, ItemStack stack) {
        this.resultingMatrix[slot] = Objects.requireNonNullElseGet(stack, () -> new ItemStack(Material.AIR));
    }
}
