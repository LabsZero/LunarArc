package org.bukkit.craftbukkit.persistence;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Persists Bukkit/Paper world PDC values inside the real Minecraft per-level
 * saved-data storage. This intentionally uses vanilla's SavedData mechanism so
 * Forge, NeoForge, Fabric, and Quilt remain authoritative for the world save.
 */
public final class CraftWorldPersistentData extends SavedData {
    private static final String NAME = "lunararc_bukkit_values";
    private static final String BUKKIT_VALUES = "BukkitValues";

    private final CraftPersistentDataContainer container;

    private CraftWorldPersistentData() {
        this.container = new CraftPersistentDataContainer();
    }

    private CraftWorldPersistentData(CompoundTag tag) {
        this();
        if (tag.contains(BUKKIT_VALUES)) {
            this.container.putAll(tag.getCompound(BUKKIT_VALUES));
        }
    }

    public static CraftWorldPersistentData get(ServerLevel level) {
        java.util.Objects.requireNonNull(level, "level");
        SavedData.Factory<CraftWorldPersistentData> factory = new SavedData.Factory<>(
                CraftWorldPersistentData::new,
                (tag, provider) -> new CraftWorldPersistentData(tag),
                null);
        return level.getDataStorage().computeIfAbsent(factory, NAME);
    }

    public CraftPersistentDataContainer container() {
        return this.container;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        java.util.Objects.requireNonNull(tag, "tag");
        if (!this.container.isEmpty()) {
            tag.put(BUKKIT_VALUES, this.container.toTagCompound());
        }
        return tag;
    }

    /**
     * PDC mutation happens through Bukkit's container API and SavedData does not
     * receive those individual mutations. Always dirty is therefore deliberate:
     * the server's normal level save cadence persists the current concrete PDC.
     */
    @Override
    public boolean isDirty() {
        return true;
    }
}
