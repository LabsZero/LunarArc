package org.bukkit.craftbukkit.v1_21_R1.inventory;

import io.ampznetwork.lunararc.common.messaging.LunarArcComponentPipeline;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import org.bukkit.FeatureFlag;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryView;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * Bukkit {@code MenuType.Typed} implementation backed by the live NMS menu type.
 * These instances populate the Paper {@code Registry.MENU} registry so that
 * {@code org.bukkit.inventory.MenuType.<clinit>} can resolve its constants
 * (e.g. {@code GENERIC_9X1}) through {@code Registry.MENU.getOrThrow(...)}.
 *
 * <p>Like Paper's own {@code CraftMenuType}, {@link #create(HumanEntity, Component)}
 * only builds the view; the caller opens it via {@link HumanEntity#openInventory}.
 */
public final class CraftMenuType<V extends InventoryView> implements org.bukkit.inventory.MenuType.Typed<V> {
    private final NamespacedKey key;
    private final MenuType<?> handle;
    private final Class<? extends InventoryView> viewClass;

    public CraftMenuType(NamespacedKey key, MenuType<?> handle, Class<? extends InventoryView> viewClass) {
        this.key = key;
        this.handle = handle;
        this.viewClass = viewClass;
    }

    public MenuType<?> getHandle() { return handle; }

    @Override public @NotNull NamespacedKey getKey() { return key; }

    @Override public @NotNull Set<FeatureFlag> requiredFeatures() { return Set.of(); }

    @Override public @NotNull Class<? extends InventoryView> getInventoryViewClass() { return viewClass; }

    @Override public @NotNull org.bukkit.inventory.MenuType.Typed<InventoryView> typed() { return typed(InventoryView.class); }

    @Override
    public <T extends InventoryView> @NotNull org.bukkit.inventory.MenuType.Typed<T> typed(@NotNull Class<T> viewType) {
        if (viewClass.isAssignableFrom(viewType)) {
            @SuppressWarnings("unchecked")
            org.bukkit.inventory.MenuType.Typed<T> typed = (org.bukkit.inventory.MenuType.Typed<T>) this;
            return typed;
        }
        throw new IllegalArgumentException("The MenuType '" + key + "' cannot be typed as " + viewType.getSimpleName());
    }

    @Override public @NotNull V create(@NotNull HumanEntity player, @NotNull String title) {
        return create(player, LegacyComponentSerializer.legacySection().deserialize(title));
    }

    @Override
    public @NotNull V create(@NotNull HumanEntity player, @NotNull Component title) {
        if (!(player instanceof org.bukkit.craftbukkit.v1_21_R1.entity.CraftPlayer craftPlayer)) {
            throw new IllegalArgumentException("The given player must be a CraftPlayer");
        }
        ServerPlayer sp = craftPlayer.getHandle();
        AbstractContainerMenu menu = handle.create(sp.nextContainerCounter(), sp.getInventory());
        menu.setTitle(LunarArcComponentPipeline.fromAdventure(title));
        menu.checkReachable = false;
        InventoryType type = inferType(menu);
        InventoryView view = new CraftInventoryView(craftPlayer, menu, null, null, type, title);
        @SuppressWarnings("unchecked")
        V result = (V) view;
        return result;
    }

    private static InventoryType inferType(AbstractContainerMenu menu) {
        if (menu instanceof net.minecraft.world.inventory.InventoryMenu) return InventoryType.CRAFTING;
        if (menu instanceof net.minecraft.world.inventory.ChestMenu) return InventoryType.CHEST;
        if (menu instanceof net.minecraft.world.inventory.CraftingMenu) return InventoryType.WORKBENCH;
        if (menu instanceof net.minecraft.world.inventory.FurnaceMenu) return InventoryType.FURNACE;
        if (menu instanceof net.minecraft.world.inventory.BlastFurnaceMenu) return InventoryType.BLAST_FURNACE;
        if (menu instanceof net.minecraft.world.inventory.SmokerMenu) return InventoryType.SMOKER;
        if (menu instanceof net.minecraft.world.inventory.AnvilMenu) return InventoryType.ANVIL;
        if (menu instanceof net.minecraft.world.inventory.EnchantmentMenu) return InventoryType.ENCHANTING;
        if (menu instanceof net.minecraft.world.inventory.BrewingStandMenu) return InventoryType.BREWING;
        if (menu instanceof net.minecraft.world.inventory.BeaconMenu) return InventoryType.BEACON;
        if (menu instanceof net.minecraft.world.inventory.HopperMenu) return InventoryType.HOPPER;
        if (menu instanceof net.minecraft.world.inventory.ShulkerBoxMenu) return InventoryType.SHULKER_BOX;
        if (menu instanceof net.minecraft.world.inventory.MerchantMenu) return InventoryType.MERCHANT;
        if (menu instanceof net.minecraft.world.inventory.LecternMenu) return InventoryType.LECTERN;
        if (menu instanceof net.minecraft.world.inventory.LoomMenu) return InventoryType.LOOM;
        if (menu instanceof net.minecraft.world.inventory.GrindstoneMenu) return InventoryType.GRINDSTONE;
        if (menu instanceof net.minecraft.world.inventory.CartographyTableMenu) return InventoryType.CARTOGRAPHY;
        if (menu instanceof net.minecraft.world.inventory.StonecutterMenu) return InventoryType.STONECUTTER;
        if (menu instanceof net.minecraft.world.inventory.SmithingMenu) return InventoryType.SMITHING;
        if (menu instanceof net.minecraft.world.inventory.DispenserMenu) return InventoryType.DISPENSER;
        if (menu instanceof net.minecraft.world.inventory.CrafterMenu) return InventoryType.CRAFTER;
        return InventoryType.CHEST;
    }
}
