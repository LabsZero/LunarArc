package org.bukkit.craftbukkit.util;

import net.minecraft.world.entity.MobCategory;
import org.bukkit.entity.SpawnCategory;

/** Loader-neutral Minecraft 1.21.1 spawn-category conversion. */
public final class CraftSpawnCategory {
    private CraftSpawnCategory() {}

    public static SpawnCategory toBukkit(MobCategory category) {
        if (category == null) throw new IllegalArgumentException("MobCategory cannot be null");
        return switch (category) {
            case MONSTER -> SpawnCategory.MONSTER;
            case CREATURE -> SpawnCategory.ANIMAL;
            case AMBIENT -> SpawnCategory.AMBIENT;
            case AXOLOTLS -> SpawnCategory.AXOLOTL;
            case WATER_CREATURE -> SpawnCategory.WATER_ANIMAL;
            case WATER_AMBIENT -> SpawnCategory.WATER_AMBIENT;
            case UNDERGROUND_WATER_CREATURE -> SpawnCategory.WATER_UNDERGROUND_CREATURE;
            case MISC -> SpawnCategory.MISC;
        };
    }

    public static MobCategory toNms(SpawnCategory category) {
        if (category == null) throw new IllegalArgumentException("SpawnCategory cannot be null");
        return switch (category) {
            case MONSTER -> MobCategory.MONSTER;
            case ANIMAL -> MobCategory.CREATURE;
            case AMBIENT -> MobCategory.AMBIENT;
            case AXOLOTL -> MobCategory.AXOLOTLS;
            case WATER_ANIMAL -> MobCategory.WATER_CREATURE;
            case WATER_AMBIENT -> MobCategory.WATER_AMBIENT;
            case WATER_UNDERGROUND_CREATURE -> MobCategory.UNDERGROUND_WATER_CREATURE;
            case MISC -> MobCategory.MISC;
        };
    }
}
