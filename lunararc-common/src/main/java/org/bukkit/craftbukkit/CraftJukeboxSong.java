package org.bukkit.craftbukkit;

import net.minecraft.network.chat.contents.TranslatableContents;
import org.bukkit.JukeboxSong;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/** Bukkit jukebox song backed directly by the dynamic 1.21.1 registry value. */
public final class CraftJukeboxSong implements JukeboxSong {
    private final NamespacedKey key;
    private final net.minecraft.world.item.JukeboxSong handle;

    public CraftJukeboxSong(@NotNull NamespacedKey key, @NotNull net.minecraft.world.item.JukeboxSong handle) {
        this.key = Objects.requireNonNull(key, "key");
        this.handle = Objects.requireNonNull(handle, "handle");
    }

    public @NotNull net.minecraft.world.item.JukeboxSong getHandle() { return this.handle; }
    @Override public @NotNull NamespacedKey getKey() { return this.key; }
    @Override public @NotNull String getTranslationKey() {
        if (this.handle.description().getContents() instanceof TranslatableContents translatable) return translatable.getKey();
        throw new UnsupportedOperationException("Jukebox song description is not translatable: " + this.key);
    }
    @Override public String toString() { return "CraftJukeboxSong[" + this.key + "]"; }
}
