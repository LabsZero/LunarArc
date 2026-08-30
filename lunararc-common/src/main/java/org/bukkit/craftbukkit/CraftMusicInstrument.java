package org.bukkit.craftbukkit;

import net.minecraft.world.item.Instrument;
import org.bukkit.MusicInstrument;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/** Concrete Bukkit view of a live 1.21.1 Minecraft instrument registry entry. */
public final class CraftMusicInstrument extends MusicInstrument {
    private final NamespacedKey key;
    private final Instrument handle;

    public CraftMusicInstrument(@NotNull NamespacedKey key, @NotNull Instrument handle) {
        this.key = Objects.requireNonNull(key, "key");
        this.handle = Objects.requireNonNull(handle, "handle");
    }

    public @NotNull Instrument getHandle() { return this.handle; }
    @Override public @NotNull NamespacedKey getKey() { return this.key; }

    @Override public boolean equals(Object other) {
        return this == other || (other instanceof MusicInstrument instrument && this.key.equals(instrument.getKey()));
    }
    @Override public int hashCode() { return this.key.hashCode(); }
    @Override public String toString() { return "CraftMusicInstrument[" + this.key + "]"; }
}
