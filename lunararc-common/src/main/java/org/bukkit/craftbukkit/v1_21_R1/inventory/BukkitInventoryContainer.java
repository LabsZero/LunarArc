package org.bukkit.craftbukkit.v1_21_R1.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

/** NMS Container adapter which keeps a Bukkit inventory as the canonical store. */
public final class BukkitInventoryContainer implements Container {
    private final Inventory inventory;
    public BukkitInventoryContainer(@NotNull Inventory inventory) { this.inventory = inventory; }
    public Inventory getBukkitInventory() { return inventory; }

    @Override public int getContainerSize() { return inventory.getSize(); }
    @Override public boolean isEmpty() { return inventory.isEmpty(); }
    @Override public net.minecraft.world.item.ItemStack getItem(int slot) {
        return slot < 0 || slot >= inventory.getSize() ? net.minecraft.world.item.ItemStack.EMPTY : CraftItemStack.asNMSCopy(inventory.getItem(slot));
    }
    @Override public net.minecraft.world.item.ItemStack removeItem(int slot, int amount) {
        if (slot < 0 || slot >= inventory.getSize() || amount <= 0) return net.minecraft.world.item.ItemStack.EMPTY;
        org.bukkit.inventory.ItemStack current = inventory.getItem(slot);
        if (current == null || current.getType().isAir()) return net.minecraft.world.item.ItemStack.EMPTY;
        int taken = Math.min(amount, current.getAmount());
        org.bukkit.inventory.ItemStack removed = current.clone();
        removed.setAmount(taken);
        int remain = current.getAmount() - taken;
        if (remain <= 0) inventory.setItem(slot, null); else { current.setAmount(remain); inventory.setItem(slot, current); }
        return CraftItemStack.asNMSCopy(removed);
    }
    @Override public net.minecraft.world.item.ItemStack removeItemNoUpdate(int slot) {
        if (slot < 0 || slot >= inventory.getSize()) return net.minecraft.world.item.ItemStack.EMPTY;
        org.bukkit.inventory.ItemStack current = inventory.getItem(slot);
        inventory.setItem(slot, null);
        return CraftItemStack.asNMSCopy(current);
    }
    @Override public void setItem(int slot, net.minecraft.world.item.ItemStack stack) {
        if (slot < 0 || slot >= inventory.getSize()) return;
        inventory.setItem(slot, stack == null || stack.isEmpty() ? null : CraftItemStack.asBukkitCopy(stack));
    }
    @Override public void setChanged() { }
    @Override public boolean stillValid(Player player) { return true; }
    @Override public void clearContent() { inventory.clear(); }
    @Override public int getMaxStackSize() { return inventory.getMaxStackSize(); }
    @Override public void setMaxStackSize(int size) { inventory.setMaxStackSize(size); }
    @Override public org.bukkit.Location getLocation() { return inventory.getLocation(); }
    @Override public org.bukkit.inventory.InventoryHolder getOwner() { return inventory.getHolder(); }
    @Override public java.util.List<net.minecraft.world.item.ItemStack> getContents() {
        java.util.List<net.minecraft.world.item.ItemStack> out = new java.util.ArrayList<>();
        for (int i = 0; i < inventory.getSize(); i++) out.add(CraftItemStack.asNMSCopy(inventory.getItem(i)));
        return out;
    }
    @Override public void onOpen(org.bukkit.craftbukkit.entity.CraftHumanEntity who) { }
    @Override public void onClose(org.bukkit.craftbukkit.entity.CraftHumanEntity who) { }
    @Override public java.util.List<org.bukkit.entity.HumanEntity> getViewers() {
        if (inventory.getHolder() instanceof org.bukkit.entity.HumanEntity holder) {
            return java.util.Collections.singletonList(holder);
        }
        return java.util.Collections.emptyList();
    }
}
