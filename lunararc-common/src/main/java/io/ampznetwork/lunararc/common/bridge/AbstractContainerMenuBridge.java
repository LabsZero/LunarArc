package io.ampznetwork.lunararc.common.bridge;

import net.minecraft.server.level.ServerPlayer;
import javax.annotation.Nullable;

public interface AbstractContainerMenuBridge {
    @Nullable ServerPlayer lunararc$getOwner();
    void lunararc$setOwner(@Nullable ServerPlayer owner);
    boolean lunararc$getCheckReachable();
    void lunararc$setCheckReachable(boolean checkReachable);

    /**
     * The Bukkit view this menu was opened with, or {@code null} for a menu vanilla opened.
     *
     * <p>CraftBukkit keeps this on the menu as {@code bukkitView} and hands the same object back
     * from getOpenInventory() for the life of the container. That identity is the whole contract a
     * plugin GUI relies on: it opens an Inventory it created, with its own InventoryHolder, and its
     * click handler asks whether the clicked inventory is that one before it acts.</p>
     */
    @Nullable org.bukkit.inventory.InventoryView lunararc$getBukkitView();

    void lunararc$setBukkitView(@Nullable org.bukkit.inventory.InventoryView view);
}
