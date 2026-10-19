package org.bukkit.craftbukkit.persistence;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Persists Bukkit/Paper world PDC values inside the real Minecraft per-level
 * saved-data storage. This intentionally uses vanilla's SavedData mechanism so
 * Forge, NeoForge, Fabric, and Quilt remain authoritative for the world save.
 */
public final class CraftWorldPersistentData extends SavedData {
    private static final String NAME = "lunararc_bukkit_values";
    private static final String BUKKIT_VALUES = "BukkitValues";

    private static final Codec<CraftWorldPersistentData> CODEC = CompoundTag.CODEC.xmap(
            CraftWorldPersistentData::new,
            CraftWorldPersistentData::toTag);
    private static final SavedDataType<CraftWorldPersistentData> TYPE = new SavedDataType<>(
            net.minecraft.resources.Identifier.fromNamespaceAndPath("lunararc", NAME), CraftWorldPersistentData::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final CraftPersistentDataContainer container;

    private CraftWorldPersistentData() {
        this.container = new CraftPersistentDataContainer();
    }

    private CraftWorldPersistentData(CompoundTag tag) {
        this();
        if (tag.contains(BUKKIT_VALUES)) {
            this.container.putAll(tag.getCompoundOrEmpty(BUKKIT_VALUES));
        }
    }

    public static CraftWorldPersistentData get(ServerLevel level) {
        java.util.Objects.requireNonNull(level, "level");
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public CraftPersistentDataContainer container() {
        return this.container;
    }

    private CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
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
