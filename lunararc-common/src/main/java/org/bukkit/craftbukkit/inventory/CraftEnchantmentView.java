package org.bukkit.craftbukkit.inventory;

import io.lunararcdevs.lunararc.common.bridge.access.EnchantmentMenuAccessBridge;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.enchantment.Enchantment;
import org.bukkit.craftbukkit.enchantments.CraftEnchantment;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.inventory.EnchantingInventory;
import org.bukkit.inventory.view.EnchantmentView;
import org.jetbrains.annotations.NotNull;

public final class CraftEnchantmentView extends CraftInventoryView implements EnchantmentView {
    private final EnchantmentMenu handle;
    private final CraftInventoryEnchanting top;

    public CraftEnchantmentView(org.bukkit.craftbukkit.entity.CraftPlayer player, EnchantmentMenu handle,
                                net.kyori.adventure.text.Component title) {
        this(player, handle, new CraftInventoryEnchanting(((EnchantmentMenuAccessBridge) handle).lunararc$getEnchantSlots()), title);
    }

    private CraftEnchantmentView(org.bukkit.craftbukkit.entity.CraftPlayer player, EnchantmentMenu handle,
                                 CraftInventoryEnchanting top, net.kyori.adventure.text.Component title) {
        super(player, handle, top, player.getInventory(), org.bukkit.event.inventory.InventoryType.ENCHANTING, title);
        this.handle = handle;
        this.top = top;
    }

    private Registry<Enchantment> registry() {
        return ((org.bukkit.craftbukkit.entity.CraftPlayer) getPlayer()).getHandle().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
    }

    @Override public @NotNull EnchantingInventory getTopInventory() { return this.top; }
    @Override public int getEnchantmentSeed() { return this.handle.getEnchantmentSeed(); }
    @Override public void setEnchantmentSeed(int seed) { ((EnchantmentMenuAccessBridge) this.handle).lunararc$getEnchantmentSeed().set(seed); }

    @Override
    public @NotNull EnchantmentOffer[] getOffers() {
        Registry<Enchantment> registry = registry();
        EnchantmentOffer[] offers = new EnchantmentOffer[3];
        for (int i = 0; i < 3; i++) {
            Holder<Enchantment> clue = this.handle.enchantClue[i] < 0 ? null : registry.getHolder(this.handle.enchantClue[i]).orElse(null);
            offers[i] = clue == null ? null
                    : new EnchantmentOffer(CraftEnchantment.minecraftHolderToBukkit(clue), this.handle.levelClue[i], this.handle.costs[i]);
        }
        return offers;
    }

    @Override
    public void setOffers(@NotNull EnchantmentOffer[] offers) {
        if (offers.length != 3) throw new IllegalArgumentException("There must be 3 offers given");
        Registry<Enchantment> registry = registry();
        for (int i = 0; i < 3; i++) {
            EnchantmentOffer offer = offers[i];
            this.handle.costs[i] = offer == null ? 0 : offer.getCost();
            this.handle.enchantClue[i] = offer == null ? -1 : registry.getId(CraftEnchantment.bukkitToMinecraft(offer.getEnchantment()));
            this.handle.levelClue[i] = offer == null ? -1 : offer.getEnchantmentLevel();
        }
    }
}
