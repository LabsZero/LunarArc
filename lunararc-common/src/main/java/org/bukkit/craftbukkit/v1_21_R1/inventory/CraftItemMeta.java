package org.bukkit.craftbukkit.v1_21_R1.inventory;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class CraftItemMeta implements ItemMeta {

    public boolean hasDestroyableKeys() { return false; }
    @Override
    public @NotNull Set<com.destroystokyo.paper.Namespaced> getDestroyableKeys() { return Collections.emptySet(); }
    @Override
    public void setDestroyableKeys(@NotNull Collection<com.destroystokyo.paper.Namespaced> canDestroy) {}
    public boolean hasPlaceableKeys() { return false; }
    @Override
    public @NotNull Set<com.destroystokyo.paper.Namespaced> getPlaceableKeys() { return Collections.emptySet(); }
    @Override
    public void setPlaceableKeys(@NotNull Collection<com.destroystokyo.paper.Namespaced> canPlaceOn) {}

    // Core data
    private Component displayName = null;
    private List<Component> lore = null;
    private Map<Enchantment, Integer> enchantments = new HashMap<>();
    private boolean unbreakable = false;
    private int customModelData = 0;
    private boolean hasCustomModelData = false;

    public CraftItemMeta() {}

    /** Construct from an NMS ItemStack, reading existing components. */
    public CraftItemMeta(ItemStack nms) {
        if (nms == null || nms.isEmpty()) return;
        // Read display name
        net.minecraft.network.chat.Component customName = nms.get(DataComponents.CUSTOM_NAME);
        if (customName != null) {
            try {
                String json = net.minecraft.network.chat.Component.Serializer.toJson(customName,
                    net.minecraft.core.RegistryAccess.EMPTY);
                displayName = GsonComponentSerializer.gson().deserialize(json);
            } catch (Throwable t) {
                displayName = Component.text(customName.getString());
            }
        }
        // Read lore
        ItemLore itemLore = nms.get(DataComponents.LORE);
        if (itemLore != null && !itemLore.lines().isEmpty()) {
            lore = new ArrayList<>();
            for (net.minecraft.network.chat.Component line : itemLore.lines()) {
                try {
                    String json = net.minecraft.network.chat.Component.Serializer.toJson(line,
                        net.minecraft.core.RegistryAccess.EMPTY);
                    lore.add(GsonComponentSerializer.gson().deserialize(json));
                } catch (Throwable t) {
                    lore.add(Component.text(line.getString()));
                }
            }
        }
        // Read unbreakable
        net.minecraft.world.item.component.Unbreakable u = nms.get(DataComponents.UNBREAKABLE);
        if (u != null) unbreakable = true;
        // Read custom model data
        net.minecraft.world.item.component.CustomModelData cmd = nms.get(DataComponents.CUSTOM_MODEL_DATA);
        if (cmd != null) {
            hasCustomModelData = true;
            customModelData = cmd.value();
        }
        // Read enchantments
        net.minecraft.world.item.enchantment.ItemEnchantments enc = nms.get(DataComponents.ENCHANTMENTS);
        if (enc != null) {
            enc.entrySet().forEach(e -> {
                try {
                    net.minecraft.resources.ResourceLocation key =
                        e.getKey().unwrapKey().map(k -> k.location()).orElse(null);
                    if (key != null) {
                        Enchantment bukkit = Enchantment.getByKey(
                            org.bukkit.NamespacedKey.fromString(key.toString()));
                        if (bukkit != null) enchantments.put(bukkit, e.getIntValue());
                    }
                } catch (Throwable ignored) {}
            });
        }
    }

    /** Apply this meta's data onto the given NMS ItemStack. */
    public void applyToNms(ItemStack nms) {
        if (nms == null || nms.isEmpty()) return;
        // Display name
        if (displayName != null) {
            try {
                String json = GsonComponentSerializer.gson().serialize(displayName);
                net.minecraft.network.chat.Component nmsComp =
                    net.minecraft.network.chat.Component.Serializer.fromJson(json,
                        net.minecraft.core.RegistryAccess.EMPTY);
                nms.set(DataComponents.CUSTOM_NAME, nmsComp);
            } catch (Throwable t) {
                nms.set(DataComponents.CUSTOM_NAME,
                    net.minecraft.network.chat.Component.literal(
                        net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(displayName)));
            }
        } else {
            nms.remove(DataComponents.CUSTOM_NAME);
        }
        // Lore
        if (lore != null && !lore.isEmpty()) {
            List<net.minecraft.network.chat.Component> lines = new ArrayList<>();
            for (Component line : lore) {
                try {
                    String json = GsonComponentSerializer.gson().serialize(line);
                    lines.add(net.minecraft.network.chat.Component.Serializer.fromJson(json,
                        net.minecraft.core.RegistryAccess.EMPTY));
                } catch (Throwable t) {
                    lines.add(net.minecraft.network.chat.Component.literal(
                        net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(line)));
                }
            }
            nms.set(DataComponents.LORE, new ItemLore(lines));
        } else {
            nms.remove(DataComponents.LORE);
        }
        // Unbreakable
        if (unbreakable) {
            nms.set(DataComponents.UNBREAKABLE, new net.minecraft.world.item.component.Unbreakable(true));
        } else {
            nms.remove(DataComponents.UNBREAKABLE);
        }
        // Custom model data
        if (hasCustomModelData) {
            nms.set(DataComponents.CUSTOM_MODEL_DATA, new net.minecraft.world.item.component.CustomModelData(customModelData));
        }
    }

    // ---- Adventure API ----

    @Override
    public @Nullable Component displayName() {
        return displayName;
    }

    @Override
    public void displayName(@Nullable Component displayName) {
        this.displayName = displayName;
    }

    @Override
    public @NotNull Component itemName() {
        return displayName != null ? displayName : Component.empty();
    }

    @Override
    public void itemName(@Nullable Component name) {
        this.displayName = name;
    }

    @Override
    public @Nullable List<Component> lore() {
        return lore;
    }

    @Override
    public void lore(@Nullable List<? extends Component> lore) {
        this.lore = lore == null ? null : new ArrayList<>(lore);
    }

    // ---- Legacy String API ----

    @Override
    public boolean hasDisplayName() {
        return displayName != null;
    }

    @Override
    public @NotNull String getDisplayName() {
        if (displayName == null) return "";
        return LegacyComponentSerializer.legacySection().serialize(displayName);
    }

    @Override
    public void setDisplayName(@Nullable String name) {
        this.displayName = name == null ? null :
            LegacyComponentSerializer.legacySection().deserialize(name);
    }

    @Override
    public boolean hasItemName() {
        return displayName != null;
    }

    @Override
    public @NotNull String getItemName() {
        return getDisplayName();
    }

    @Override
    public void setItemName(@Nullable String name) {
        setDisplayName(name);
    }

    @Override
    public boolean hasLore() {
        return lore != null && !lore.isEmpty();
    }

    @Override
    public @Nullable List<String> getLore() {
        if (lore == null) return null;
        List<String> out = new ArrayList<>();
        for (Component c : lore) out.add(LegacyComponentSerializer.legacySection().serialize(c));
        return out;
    }

    @Override
    public void setLore(@Nullable List<String> lore) {
        if (lore == null) { this.lore = null; return; }
        this.lore = new ArrayList<>();
        for (String s : lore) this.lore.add(LegacyComponentSerializer.legacySection().deserialize(s));
    }

    // ---- Enchantments ----

    @Override
    public boolean hasEnchants() {
        return !enchantments.isEmpty();
    }

    @Override
    public boolean hasEnchant(@NotNull Enchantment ench) {
        return enchantments.containsKey(ench);
    }

    @Override
    public int getEnchantLevel(@NotNull Enchantment ench) {
        return enchantments.getOrDefault(ench, 0);
    }

    @Override
    public @NotNull Map<Enchantment, Integer> getEnchants() {
        return Collections.unmodifiableMap(enchantments);
    }

    @Override
    public boolean addEnchant(@NotNull Enchantment ench, int level, boolean ignoreLevelRestriction) {
        enchantments.put(ench, level);
        return true;
    }

    @Override
    public boolean removeEnchant(@NotNull Enchantment ench) {
        return enchantments.remove(ench) != null;
    }

    @Override
    public void removeEnchantments() {
        enchantments.clear();
    }

    public boolean removeEnchants(@NotNull Enchantment... enchantments) {
        boolean changed = false;
        for (Enchantment e : enchantments) changed |= removeEnchant(e);
        return changed;
    }

    @Override
    public boolean hasConflictingEnchant(@NotNull Enchantment ench) {
        return false;
    }

    public boolean isEnchantmentGlintOverrideSet() { return false; }
    @Override
    public boolean hasEnchantmentGlintOverride() { return false; }
    @Override
    public @Nullable Boolean getEnchantmentGlintOverride() { return null; }
    @Override
    public void setEnchantmentGlintOverride(@Nullable Boolean override) {}

    // ---- Custom model data ----

    @Override
    public boolean hasCustomModelData() {
        return hasCustomModelData;
    }

    @Override
    public int getCustomModelData() {
        return customModelData;
    }

    @Override
    public void setCustomModelData(@Nullable Integer data) {
        if (data == null) { hasCustomModelData = false; customModelData = 0; }
        else { hasCustomModelData = true; customModelData = data; }
    }

    // ---- Unbreakable ----

    @Override
    public boolean isUnbreakable() {
        return unbreakable;
    }

    @Override
    public void setUnbreakable(boolean unbreakable) {
        this.unbreakable = unbreakable;
    }

    // ---- Attributes ----

    @Override
    public boolean hasAttributeModifiers() { return false; }

    @Override
    public @Nullable com.google.common.collect.Multimap<Attribute, AttributeModifier> getAttributeModifiers() {
        return null;
    }

    @Override
    public @NotNull com.google.common.collect.Multimap<Attribute, AttributeModifier> getAttributeModifiers(
            @NotNull EquipmentSlot slot) {
        return com.google.common.collect.HashMultimap.create();
    }

    public @NotNull com.google.common.collect.Multimap<Attribute, AttributeModifier> getAttributeModifiers(
            @NotNull EquipmentSlotGroup group) {
        return com.google.common.collect.HashMultimap.create();
    }

    @Override
    public @Nullable Collection<AttributeModifier> getAttributeModifiers(@NotNull Attribute attribute) {
        return null;
    }

    @Override
    public boolean addAttributeModifier(@NotNull Attribute attribute, @NotNull AttributeModifier modifier) {
        return false;
    }

    @Override
    public void setAttributeModifiers(@Nullable com.google.common.collect.Multimap<Attribute, AttributeModifier> modifiers) {}

    @Override
    public boolean removeAttributeModifier(@NotNull Attribute attribute) { return false; }
    @Override
    public boolean removeAttributeModifier(@NotNull EquipmentSlot slot) { return false; }
    public boolean removeAttributeModifier(@NotNull EquipmentSlotGroup group) { return false; }
    @Override
    public boolean removeAttributeModifier(@NotNull Attribute attribute, @NotNull AttributeModifier modifier) { return false; }

    // ---- Other ----

    @Override
    public boolean hasLocalizedName() { return false; }
    @Override
    public @NotNull String getLocalizedName() { return getDisplayName(); }
    @Override
    public void setLocalizedName(@Nullable String name) {}

    @Override
    public @NotNull Set<org.bukkit.Material> getCanDestroy() { return Collections.emptySet(); }
    @Override
    public void setCanDestroy(@Nullable Set<org.bukkit.Material> materials) {}
    @Override
    public @NotNull Set<org.bukkit.Material> getCanPlaceOn() { return Collections.emptySet(); }
    @Override
    public void setCanPlaceOn(@Nullable Set<org.bukkit.Material> materials) {}

    public boolean hasCustomTags() { return false; }
    @Override
    public @NotNull org.bukkit.inventory.meta.tags.CustomItemTagContainer getCustomTagContainer() {
        return (org.bukkit.inventory.meta.tags.CustomItemTagContainer)
            java.lang.reflect.Proxy.newProxyInstance(
                org.bukkit.inventory.meta.tags.CustomItemTagContainer.class.getClassLoader(),
                new Class<?>[]{ org.bukkit.inventory.meta.tags.CustomItemTagContainer.class },
                (proxy, method, args) -> {
                    if (method.getReturnType() == boolean.class) return false;
                    if (method.getReturnType() == java.util.Map.class) return Collections.emptyMap();
                    return null;
                });
    }

    public int getVersion() { return 0; }
    public void setVersion(int version) {}

    @Override
    public @NotNull String getAsString() { return "{}"; }

    @Override
    public @NotNull String getAsComponentString() { return "{}"; }

    @Override
    public boolean hasRarity() { return false; }
    @Override
    public org.bukkit.inventory.ItemRarity getRarity() { return org.bukkit.inventory.ItemRarity.COMMON; }
    @Override
    public void setRarity(@Nullable org.bukkit.inventory.ItemRarity rarity) {}
    public boolean isGlider() { return false; }
    public void setGlider(boolean glider) {}
    @Override
    public boolean hasTool() { return false; }
    @Override
    public @Nullable org.bukkit.inventory.meta.components.ToolComponent getTool() { return null; }
    @Override
    public void setTool(@Nullable org.bukkit.inventory.meta.components.ToolComponent tool) {}
    @Override
    public boolean hasJukeboxPlayable() { return false; }
    @Override
    public @Nullable org.bukkit.inventory.meta.components.JukeboxPlayableComponent getJukeboxPlayable() { return null; }
    @Override
    public void setJukeboxPlayable(@Nullable org.bukkit.inventory.meta.components.JukeboxPlayableComponent jukeboxPlayable) {}
    @Override
    public boolean hasFood() { return false; }
    @Override
    public @NotNull org.bukkit.inventory.meta.components.FoodComponent getFood() {
        return (org.bukkit.inventory.meta.components.FoodComponent) java.lang.reflect.Proxy.newProxyInstance(
            org.bukkit.inventory.meta.components.FoodComponent.class.getClassLoader(),
            new Class<?>[]{ org.bukkit.inventory.meta.components.FoodComponent.class },
            (proxy, method, args) -> {
                if (method.getReturnType() == boolean.class) return false;
                if (method.getReturnType() == int.class) return 0;
                if (method.getReturnType() == float.class) return 0.0f;
                return null;
            });
    }
    @Override
    public void setFood(@Nullable org.bukkit.inventory.meta.components.FoodComponent food) {}
    public boolean hasUseRemainder() { return false; }
    public @Nullable ItemStack getUseRemainder() { return null; }
    public void setUseRemainder(@Nullable ItemStack remainder) {}
    @Override
    public boolean hasMaxStackSize() { return false; }
    @Override
    public int getMaxStackSize() { return 64; }
    @Override
    public void setMaxStackSize(@Nullable Integer max) {}
    public boolean hasTooltipStyle() { return false; }
    public @Nullable NamespacedKey getTooltipStyle() { return null; }
    public void setTooltipStyle(@Nullable NamespacedKey key) {}
    @Override
    public boolean isHideTooltip() { return false; }
    @Override
    public void setHideTooltip(boolean hide) {}
    public boolean hasDyedColor() { return false; }
    public boolean hasCustomName() { return hasDisplayName(); }
    public boolean hasRepairCost() { return false; }
    public int getRepairCost() { return 0; }
    public boolean isFireResistant() { return false; }
    public void setFireResistant(boolean fireResistant) {}
    public net.md_5.bungee.api.chat.BaseComponent[] getDisplayNameComponent() { return null; }
    public void setDisplayNameComponent(net.md_5.bungee.api.chat.BaseComponent[] component) {}
    @Nullable public java.util.List<net.md_5.bungee.api.chat.BaseComponent[]> getLoreComponents() { return null; }
    public void setLoreComponents(@Nullable java.util.List<net.md_5.bungee.api.chat.BaseComponent[]> lore) {}
    @Override
    public void addItemFlags(@NotNull org.bukkit.inventory.ItemFlag... itemFlags) {}
    @Override
    public void removeItemFlags(@NotNull org.bukkit.inventory.ItemFlag... itemFlags) {}
    @Override
    public @NotNull Set<org.bukkit.inventory.ItemFlag> getItemFlags() { return Collections.emptySet(); }
    @Override
    public boolean hasItemFlag(@NotNull org.bukkit.inventory.ItemFlag flag) { return false; }
    @Override
    public @NotNull PersistentDataContainer getPersistentDataContainer() {
        return new org.bukkit.craftbukkit.v1_21_R1.persistence.CraftPersistentDataContainer();
    }

    // ---- Standard Object ----

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new LinkedHashMap<>();
        if (hasDisplayName()) map.put("display-name", getDisplayName());
        if (hasLore()) map.put("lore", getLore());
        if (hasEnchants()) map.put("enchants", new HashMap<>(enchantments));
        return map;
    }

    @Override
    public @NotNull CraftItemMeta clone() {
        try {
            CraftItemMeta clone = (CraftItemMeta) super.clone();
            clone.enchantments = new HashMap<>(enchantments);
            if (lore != null) clone.lore = new ArrayList<>(lore);
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new Error(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CraftItemMeta other)) return false;
        return Objects.equals(displayName, other.displayName)
            && Objects.equals(lore, other.lore)
            && Objects.equals(enchantments, other.enchantments)
            && unbreakable == other.unbreakable
            && hasCustomModelData == other.hasCustomModelData
            && customModelData == other.customModelData;
    }

    @Override
    public int hashCode() {
        return Objects.hash(displayName, lore, enchantments, unbreakable, hasCustomModelData, customModelData);
    }
}
