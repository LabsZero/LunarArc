package org.bukkit.craftbukkit.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Concrete Bukkit crafting inventory backed by the real vanilla crafting/result containers. */
public final class CraftInventoryCrafting extends CraftNMSInventory implements CraftingInventory {
    private final CraftingContainer matrix;
    private final Container result;
    private final @Nullable RecipeHolder<CraftingRecipe> recipe;

    public CraftInventoryCrafting(CraftingContainer matrix, Container result,
                                  @Nullable HumanEntity owner,
                                  @Nullable RecipeHolder<CraftingRecipe> recipe) {
        super(matrix, owner, InventoryType.WORKBENCH);
        this.matrix = java.util.Objects.requireNonNull(matrix, "matrix");
        this.result = java.util.Objects.requireNonNull(result, "result");
        this.recipe = recipe;
    }

    public CraftingContainer getMatrixInventory() {
        return this.matrix;
    }

    public Container getResultInventory() {
        return this.result;
    }

    @Override
    public int getSize() {
        return this.result.getContainerSize() + this.matrix.getContainerSize();
    }

    @Override
    public @Nullable ItemStack getItem(int index) {
        if (index < 0 || index >= getSize()) return null;
        net.minecraft.world.item.ItemStack stack = index < this.result.getContainerSize()
                ? this.result.getItem(index)
                : this.matrix.getItem(index - this.result.getContainerSize());
        return stack.isEmpty() ? null : CraftItemStack.asBukkitCopy(stack);
    }

    @Override
    public void setItem(int index, @Nullable ItemStack item) {
        if (index < 0 || index >= getSize()) return;
        if (index < this.result.getContainerSize()) {
            this.result.setItem(index, CraftItemStack.asNMSCopy(item));
            this.result.setChanged();
        } else {
            this.matrix.setItem(index - this.result.getContainerSize(), CraftItemStack.asNMSCopy(item));
            this.matrix.setChanged();
        }
    }

    @Override
    public @NotNull ItemStack[] getContents() {
        ItemStack[] out = new ItemStack[getSize()];
        for (int i = 0; i < out.length; i++) out[i] = getItem(i);
        return out;
    }

    @Override
    public void setContents(@NotNull ItemStack[] items) {
        if (items.length > getSize()) throw new IllegalArgumentException("items array too large");
        for (int i = 0; i < getSize(); i++) setItem(i, i < items.length ? items[i] : null);
    }

    @Override
    public @NotNull ItemStack[] getMatrix() {
        ItemStack[] out = new ItemStack[this.matrix.getContainerSize()];
        for (int i = 0; i < out.length; i++) {
            net.minecraft.world.item.ItemStack stack = this.matrix.getItem(i);
            out[i] = stack.isEmpty() ? null : CraftItemStack.asBukkitCopy(stack);
        }
        return out;
    }

    @Override
    public void setMatrix(@NotNull ItemStack[] contents) {
        if (contents.length > this.matrix.getContainerSize()) {
            throw new IllegalArgumentException("Invalid crafting matrix size " + contents.length);
        }
        for (int i = 0; i < this.matrix.getContainerSize(); i++) {
            this.matrix.setItem(i, i < contents.length ? CraftItemStack.asNMSCopy(contents[i]) : net.minecraft.world.item.ItemStack.EMPTY);
        }
        this.matrix.setChanged();
    }

    @Override
    public @Nullable ItemStack getResult() {
        net.minecraft.world.item.ItemStack stack = this.result.getItem(0);
        return stack.isEmpty() ? null : CraftItemStack.asBukkitCopy(stack);
    }

    @Override
    public void setResult(@Nullable ItemStack item) {
        this.result.setItem(0, CraftItemStack.asNMSCopy(item));
        this.result.setChanged();
    }

    @Override
    public @Nullable Recipe getRecipe() {
        return this.recipe == null ? null : CraftRecipeAdapter.toBukkit(this.recipe);
    }
}
