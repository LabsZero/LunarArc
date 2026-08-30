package org.bukkit.craftbukkit.inventory;

import net.minecraft.world.inventory.MerchantMenu;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.view.MerchantView;
import org.jetbrains.annotations.NotNull;

public final class CraftMerchantView extends CraftInventoryView implements MerchantView {
    private final CraftMerchantInventory top;

    public CraftMerchantView(org.bukkit.craftbukkit.entity.CraftPlayer player, MerchantMenu handle,
                             net.kyori.adventure.text.Component title) {
        this(player, handle, new CraftMerchantInventory(handle, player), title);
    }

    private CraftMerchantView(org.bukkit.craftbukkit.entity.CraftPlayer player, MerchantMenu handle,
                              CraftMerchantInventory top, net.kyori.adventure.text.Component title) {
        super(player, handle, top, player.getInventory(), InventoryType.MERCHANT, title);
        this.top = top;
    }

    @Override public @NotNull MerchantInventory getTopInventory() { return this.top; }
    @Override public @NotNull Merchant getMerchant() { return this.top.getMerchant(); }
}
