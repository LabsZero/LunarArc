package org.bukkit.craftbukkit.inventory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.jetbrains.annotations.NotNull;

/** Enchanted-book metadata backed by the STORED_ENCHANTMENTS data component. */
@org.bukkit.configuration.serialization.DelegateDeserialization(SerializableMeta.class)
public final class CraftMetaEnchantedBook extends CraftItemMeta implements EnchantmentStorageMeta {
    private Map<Enchantment, Integer> stored = new HashMap<>();

    public CraftMetaEnchantedBook() { super(); }

    public CraftMetaEnchantedBook(ItemStack nms) {
        super(nms);
        if (nms == null || nms.isEmpty()) return;
        ItemEnchantments enchantments = nms.get(DataComponents.STORED_ENCHANTMENTS);
        if (enchantments == null) return;
        enchantments.entrySet().forEach(entry -> {
            Identifier id = entry.getKey().unwrapKey().map(ResourceKey::identifier).orElse(null);
            if (id == null) return;
            Enchantment bukkit = Enchantment.getByKey(new org.bukkit.NamespacedKey(id.getNamespace(), id.getPath()));
            if (bukkit != null) stored.put(bukkit, entry.getIntValue());
        });
    }

    @Override
    public void applyToNms(ItemStack nms) {
        super.applyToNms(nms);
        if (stored.isEmpty()) {
            nms.remove(DataComponents.STORED_ENCHANTMENTS);
            return;
        }
        net.minecraft.core.Registry<net.minecraft.world.item.enchantment.Enchantment> registry =
                ((org.bukkit.craftbukkit.CraftServer) org.bukkit.Bukkit.getServer()).getServer().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        for (Map.Entry<Enchantment, Integer> entry : stored.entrySet()) {
            ResourceKey<net.minecraft.world.item.enchantment.Enchantment> key = ResourceKey.create(Registries.ENCHANTMENT,
                    Identifier.fromNamespaceAndPath(entry.getKey().getKey().getNamespace(), entry.getKey().getKey().getKey()));
            registry.get(key).ifPresent(holder -> mutable.set(holder, entry.getValue()));
        }
        nms.set(DataComponents.STORED_ENCHANTMENTS, mutable.toImmutable());
    }

    @Override public boolean hasStoredEnchants() { return !stored.isEmpty(); }
    @Override public boolean hasStoredEnchant(@NotNull Enchantment ench) { return stored.containsKey(Objects.requireNonNull(ench, "ench")); }
    @Override public int getStoredEnchantLevel(@NotNull Enchantment ench) { return stored.getOrDefault(Objects.requireNonNull(ench, "ench"), 0); }
    @Override public @NotNull Map<Enchantment, Integer> getStoredEnchants() { return Map.copyOf(stored); }

    @Override
    public boolean addStoredEnchant(@NotNull Enchantment ench, int level, boolean ignoreLevelRestriction) {
        Objects.requireNonNull(ench, "ench");
        if (!ignoreLevelRestriction && (level < ench.getStartLevel() || level > ench.getMaxLevel())) return false;
        Integer old = stored.put(ench, level);
        return old == null || old != level;
    }

    @Override public boolean removeStoredEnchant(@NotNull Enchantment ench) { return stored.remove(Objects.requireNonNull(ench, "ench")) != null; }

    @Override
    public boolean hasConflictingStoredEnchant(@NotNull Enchantment ench) {
        Objects.requireNonNull(ench, "ench");
        for (Enchantment existing : stored.keySet()) if (existing.conflictsWith(ench)) return true;
        return false;
    }

    @Override
    public CraftMetaEnchantedBook clone() {
        CraftMetaEnchantedBook clone = (CraftMetaEnchantedBook) super.clone();
        clone.stored = new HashMap<>(stored);
        return clone;
    }
}
