package org.bukkit.craftbukkit.v1_21_R1.inventory;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Live Bukkit view over an NMS container menu.  This deliberately keeps the
 * menu as the source of truth so plugin writes are reflected in the client and
 * in the underlying Minecraft inventories instead of a detached shadow copy.
 */
public final class CraftInventoryView implements InventoryView {
    private final org.bukkit.craftbukkit.v1_21_R1.entity.CraftPlayer player;
    private final AbstractContainerMenu handle;
    private final Inventory top;
    private final Inventory bottom;
    private final InventoryType type;
    private final net.kyori.adventure.text.Component title;

    public CraftInventoryView(org.bukkit.craftbukkit.v1_21_R1.entity.CraftPlayer player,
                              AbstractContainerMenu handle,
                              @Nullable Inventory top,
                              @Nullable Inventory bottom,
                              @NotNull InventoryType type,
                              @NotNull net.kyori.adventure.text.Component title) {
        this.player = player;
        this.handle = handle;
        this.bottom = bottom != null ? bottom : player.getInventory();
        // PlayerInventory exposes 41 Bukkit slots (36 storage + 4 armour + offhand),
        // while ordinary container menus only append the 36 storage/hotbar slots.
        // The player's own CRAFTING view is the exception: its NMS menu also exposes
        // armour/offhand, leaving exactly the 5-slot crafting inventory as the top.
        int appendedPlayerSlots = type == InventoryType.CRAFTING ? 41 : 36;
        int inferredTop = Math.max(0, handle.slots.size() - appendedPlayerSlots);
        this.top = top != null ? top : new MenuSlotInventory(handle, inferredTop, type, player);
        this.type = type;
        this.title = title;
    }

    public AbstractContainerMenu getHandle() { return handle; }

    @Override public @NotNull Inventory getTopInventory() { return top; }
    @Override public @NotNull Inventory getBottomInventory() { return bottom; }
    @Override public @NotNull HumanEntity getPlayer() { return player; }
    @Override public @NotNull InventoryType getType() { return type; }

    @Override
    public void setItem(int slot, @Nullable ItemStack item) {
        if (slot == OUTSIDE || slot < 0 || slot >= handle.slots.size()) return;
        handle.getSlot(slot).set(CraftItemStack.asNMSCopy(item));
        handle.broadcastChanges();
    }

    @Override
    public @Nullable ItemStack getItem(int slot) {
        if (slot == OUTSIDE || slot < 0 || slot >= handle.slots.size()) return null;
        return CraftItemStack.asBukkitCopy(handle.getSlot(slot).getItem());
    }

    @Override public void setCursor(@Nullable ItemStack item) {
        handle.setCarried(CraftItemStack.asNMSCopy(item));
        handle.broadcastCarriedItem();
    }
    @Override public @NotNull ItemStack getCursor() { return CraftItemStack.asBukkitCopy(handle.getCarried()); }

    @Override
    public @Nullable Inventory getInventory(int rawSlot) {
        if (rawSlot == OUTSIDE || rawSlot < 0 || rawSlot >= countSlots()) return null;
        return rawSlot < top.getSize() ? top : bottom;
    }

    @Override
    public int convertSlot(int rawSlot) {
        if (rawSlot < top.getSize()) return rawSlot;

        // The default player crafting menu interleaves armour and offhand around
        // the normal 36 inventory slots. Match CraftBukkit's PlayerInventory slot
        // numbering instead of treating the lower inventory as contiguous.
        if (type == InventoryType.CRAFTING && bottom.getType() == InventoryType.PLAYER) {
            if (rawSlot >= 5 && rawSlot <= 8) return 39 - (rawSlot - 5); // head..feet
            if (rawSlot >= 9 && rawSlot <= 35) return rawSlot;          // main 9..35
            if (rawSlot >= 36 && rawSlot <= 44) return rawSlot - 36;   // hotbar 0..8
            if (rawSlot == 45) return 40;                              // offhand
            return rawSlot - top.getSize();
        }

        int slot = rawSlot - top.getSize();
        // Vanilla container menus put the player's main inventory before the hotbar.
        // Bukkit PlayerInventory uses 0-8 for the hotbar and 9-35 for main.
        if (bottom.getType() == InventoryType.PLAYER && slot >= 0 && slot < 36) {
            if (slot < 27) return slot + 9;
            return slot - 27;
        }
        return slot;
    }

    @Override
    public @NotNull InventoryType.SlotType getSlotType(int slot) {
        if (slot < 0 || slot >= handle.slots.size()) return InventoryType.SlotType.OUTSIDE;
        if (slot >= top.getSize()) return InventoryType.SlotType.CONTAINER;
        return switch (type) {
            case FURNACE, BLAST_FURNACE, SMOKER -> slot == 2 ? InventoryType.SlotType.RESULT : (slot == 1 ? InventoryType.SlotType.FUEL : InventoryType.SlotType.CRAFTING);
            case CRAFTING, WORKBENCH -> slot == 0 ? InventoryType.SlotType.RESULT : InventoryType.SlotType.CRAFTING;
            case MERCHANT -> slot == 2 ? InventoryType.SlotType.RESULT : InventoryType.SlotType.CRAFTING;
            case ANVIL, SMITHING -> slot == top.getSize() - 1 ? InventoryType.SlotType.RESULT : InventoryType.SlotType.CRAFTING;
            default -> InventoryType.SlotType.CONTAINER;
        };
    }

    @Override public void close() { player.closeInventory(); }
    @Override public int countSlots() { return handle.slots.size(); }

    @Override
    public boolean setProperty(@NotNull Property prop, int value) {
        if (prop.getType() != type) return false;
        try {
            handle.setData(prop.getId(), value);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override public @NotNull net.kyori.adventure.text.Component title() { return title; }
    @Override @Deprecated public @NotNull String getTitle() {
        return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(title);
    }
    @Override @Deprecated public @NotNull String getOriginalTitle() { return getTitle(); }
    @Override @Deprecated public void setTitle(@NotNull String title) {
        throw new UnsupportedOperationException("Inventory view titles cannot be changed after opening");
    }

    /** Inventory facade for the menu's upper/raw slots. */
    private static final class MenuSlotInventory extends CraftInventory {
        private final AbstractContainerMenu menu;
        private final int size;
        private final org.bukkit.inventory.InventoryHolder owner;

        MenuSlotInventory(AbstractContainerMenu menu, int size, InventoryType type,
                          org.bukkit.inventory.InventoryHolder owner) {
            super(owner, Math.max(1, size), type, net.kyori.adventure.text.Component.text(type.name()));
            this.menu = menu;
            this.size = Math.max(0, size);
            this.owner = owner;
        }
        @Override public int getSize() { return size; }
        @Override public @Nullable ItemStack getItem(int index) {
            if (index < 0 || index >= size || index >= menu.slots.size()) return null;
            return CraftItemStack.asBukkitCopy(menu.getSlot(index).getItem());
        }
        @Override public void setItem(int index, @Nullable ItemStack item) {
            if (index < 0 || index >= size || index >= menu.slots.size()) return;
            menu.getSlot(index).set(CraftItemStack.asNMSCopy(item));
            menu.broadcastChanges();
        }
        @Override public @NotNull ItemStack[] getContents() {
            ItemStack[] out = new ItemStack[size];
            for (int i = 0; i < size; i++) out[i] = getItem(i);
            return out;
        }
        @Override public void setContents(@NotNull ItemStack[] items) {
            if (items.length > size) throw new IllegalArgumentException("items array too large");
            for (int i = 0; i < size; i++) setItem(i, i < items.length ? items[i] : null);
        }
        @Override public @NotNull java.util.List<HumanEntity> getViewers() {
            return java.util.Collections.singletonList((HumanEntity) owner);
        }
    }
}
