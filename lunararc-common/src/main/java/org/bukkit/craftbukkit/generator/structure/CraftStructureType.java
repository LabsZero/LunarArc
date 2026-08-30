package org.bukkit.craftbukkit.generator.structure;

import org.bukkit.NamespacedKey;
import org.bukkit.generator.structure.StructureType;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/** Concrete wrapper around the loader-owned Minecraft StructureType. */
public final class CraftStructureType extends StructureType {
    private final NamespacedKey key;
    private final net.minecraft.world.level.levelgen.structure.StructureType<?> handle;

    public CraftStructureType(@NotNull NamespacedKey key,
                              @NotNull net.minecraft.world.level.levelgen.structure.StructureType<?> handle) {
        this.key = Objects.requireNonNull(key, "key");
        this.handle = Objects.requireNonNull(handle, "handle");
    }

    public @NotNull net.minecraft.world.level.levelgen.structure.StructureType<?> getHandle() { return this.handle; }
    @Override public @NotNull NamespacedKey getKey() { return this.key; }
    @Override public boolean equals(Object other) {
        return this == other || (other instanceof StructureType type && this.key.equals(type.getKey()));
    }
    @Override public int hashCode() { return this.key.hashCode(); }
    @Override public String toString() { return "CraftStructureType[" + this.key + "]"; }
}
