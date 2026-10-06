package org.bukkit.craftbukkit.inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;

@SerializableAs("ItemMeta")
public final class SerializableMeta implements ConfigurationSerializable {
    private static final String[] LEGACY_TYPES = {
            "CraftItemMeta", "CraftMetaItem", "CraftMetaArmor", "CraftMetaBanner", "CraftMetaBook", "CraftMetaBookSigned",
            "CraftMetaCharge", "CraftMetaColorableArmor", "CraftMetaFirework", "CraftMetaPotion", "CraftMetaSkull"};

    private SerializableMeta() {
    }

    public static void register() {
        ConfigurationSerialization.registerClass(SerializableMeta.class);
        for (String type : LEGACY_TYPES) {
            ConfigurationSerialization.registerClass(SerializableMeta.class, "org.bukkit.craftbukkit.inventory." + type);
        }
    }

    @Override
    public Map<String, Object> serialize() {
        throw new UnsupportedOperationException("SerializableMeta only deserializes");
    }

    public static ItemMeta deserialize(Map<String, Object> map) {
        CraftItemMeta meta = new CraftItemMeta();
        Component name = component(first(map, "display-name", "displayName"));
        if (name != null) meta.displayName(name);
        Component itemName = component(first(map, "item-name"));
        if (itemName != null) meta.itemName(itemName);
        if (first(map, "lore") instanceof List<?> lines) {
            List<Component> lore = new ArrayList<>();
            for (Object line : lines) {
                Component component = component(line);
                if (component != null) lore.add(component);
            }
            meta.lore(lore);
        }
        if (first(map, "enchants") instanceof Map<?, ?> enchants) {
            for (Map.Entry<?, ?> entry : enchants.entrySet()) {
                Enchantment enchantment = enchantment(String.valueOf(entry.getKey()));
                if (enchantment != null && entry.getValue() instanceof Number level) {
                    meta.addEnchant(enchantment, level.intValue(), true);
                }
            }
        }
        if (first(map, "damage", "Damage") instanceof Number damage) meta.setDamage(damage.intValue());
        if (first(map, "max-damage") instanceof Number maxDamage) meta.setMaxDamage(maxDamage.intValue());
        if (first(map, "custom-model-data") instanceof Number model) meta.setCustomModelData(model.intValue());
        if (first(map, "unbreakable", "Unbreakable") instanceof Boolean unbreakable) meta.setUnbreakable(unbreakable);
        if (first(map, "ItemFlags", "item-flags") instanceof List<?> flags) {
            for (Object flag : flags) {
                try {
                    meta.addItemFlags(ItemFlag.valueOf(String.valueOf(flag)));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return meta;
    }

    private static Object first(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null) return value;
        }
        return null;
    }

    private static Component component(Object value) {
        if (value instanceof Component component) return component;
        if (!(value instanceof String text) || text.isEmpty()) return null;
        if (text.startsWith("{") || text.startsWith("[") || text.startsWith("\"")) {
            try {
                return GsonComponentSerializer.gson().deserialize(text);
            } catch (RuntimeException notJson) {
                return LegacyComponentSerializer.legacySection().deserialize(text);
            }
        }
        return LegacyComponentSerializer.legacySection().deserialize(text);
    }

    private static Enchantment enchantment(String key) {
        NamespacedKey namespaced = NamespacedKey.fromString(key.toLowerCase(java.util.Locale.ROOT));
        Enchantment enchantment = namespaced == null ? null : Enchantment.getByKey(namespaced);
        return enchantment != null ? enchantment : Enchantment.getByName(key);
    }
}
