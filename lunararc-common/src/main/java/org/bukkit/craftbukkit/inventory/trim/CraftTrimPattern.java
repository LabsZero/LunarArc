package org.bukkit.craftbukkit.inventory.trim;

import net.minecraft.network.chat.contents.TranslatableContents;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/** Concrete trim pattern backed by the active dynamic Minecraft registry. */
public final class CraftTrimPattern implements TrimPattern {
    private final NamespacedKey key;
    private final net.minecraft.world.item.armortrim.TrimPattern handle;

    public CraftTrimPattern(@NotNull NamespacedKey key,
                            @NotNull net.minecraft.world.item.armortrim.TrimPattern handle) {
        this.key = Objects.requireNonNull(key, "key");
        this.handle = Objects.requireNonNull(handle, "handle");
    }

    public @NotNull net.minecraft.world.item.armortrim.TrimPattern getHandle() { return this.handle; }
    @Override public @NotNull NamespacedKey getKey() { return this.key; }
    @Override public @NotNull String getTranslationKey() {
        if (this.handle.description().getContents() instanceof TranslatableContents translatable) return translatable.getKey();
        throw new UnsupportedOperationException("Trim pattern description is not translatable: " + this.key);
    }
    @Override public net.kyori.adventure.text.Component description() {
        return io.papermc.paper.adventure.PaperAdventure.asAdventure(this.handle.description());
    }
    @Override public String toString() { return "CraftTrimPattern[" + this.key + "]"; }
}
