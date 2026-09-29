package org.bukkit.craftbukkit.util;

import org.bukkit.Material;
import org.bukkit.material.MaterialData;

public final class CraftLegacy {
    private static final int LEGACY_START = Material.LEGACY_AIR.ordinal();
    private static final int LEGACY_COUNT = countLegacy();

    private CraftLegacy() {
    }

    private static int countLegacy() {
        int count = 0;
        for (Material material : Material.values()) {
            if (material.isLegacy()) count++;
        }
        return count;
    }

    public static Material fromLegacy(Material material) {
        return material.isLegacy() ? org.bukkit.craftbukkit.legacy.CraftLegacy.fromLegacy(material) : material;
    }

    public static Material fromLegacy(MaterialData materialData) {
        return org.bukkit.craftbukkit.legacy.CraftLegacy.fromLegacy(materialData);
    }

    public static Material[] modern_values() {
        Material[] all = Material.values();
        Material[] modern = new Material[all.length - LEGACY_COUNT];
        System.arraycopy(all, 0, modern, 0, LEGACY_START);
        System.arraycopy(all, LEGACY_START + LEGACY_COUNT, modern, LEGACY_START, all.length - LEGACY_START - LEGACY_COUNT);
        return modern;
    }

    public static int modern_ordinal(Material material) {
        if (material.isLegacy()) {
            throw new NoSuchFieldError("Legacy field ordinal: " + material);
        }
        int ordinal = material.ordinal();
        return ordinal < LEGACY_START ? ordinal : ordinal - LEGACY_COUNT;
    }
}
