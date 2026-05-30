package io.ampznetwork.lunararc.common.server;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Iterator;

/**
 * RegistryAccess implementation for LunarArc.
 *
 * getRegistry(Class) delegates to the running server when available.
 * getRegistry(RegistryKey) returns a safe empty stub — RegistryKey does not
 * expose the backing Class in Paper 1.21.1-R0.1-SNAPSHOT without internal
 * access, so we cannot map key → class safely at compile time.
 */
public class LunarArcRegistryAccess implements RegistryAccess {
    public static final RegistryAccess INSTANCE = new LunarArcRegistryAccess();

    private LunarArcRegistryAccess() {}

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Keyed> @NotNull Registry<T> getRegistry(@NotNull RegistryKey<T> key) {
        return (Registry<T>) EMPTY_REGISTRY;
    }

    @Override
    public <T extends Keyed> @NotNull Registry<T> getRegistry(@NotNull Class<T> type) {
        org.bukkit.Server server = org.bukkit.Bukkit.getServer();
        if (server != null) {
            Registry<T> reg = server.getRegistry(type);
            if (reg != null) return reg;
        }
        //noinspection unchecked
        return (Registry<T>) EMPTY_REGISTRY;
    }

    private static final Registry<Keyed> EMPTY_REGISTRY = new Registry<>() {
        @Override public @Nullable Keyed get(@NotNull NamespacedKey key) { return null; }
        @Override public @NotNull Keyed getOrThrow(@NotNull NamespacedKey key) { throw new java.util.NoSuchElementException(key.toString()); }
        @Override public @Nullable NamespacedKey getKey(@NotNull Keyed value) { return null; }
        @Override public @NotNull Iterator<Keyed> iterator() { return Collections.emptyIterator(); }
        @Override public @NotNull java.util.stream.Stream<Keyed> stream() { return java.util.stream.Stream.empty(); }
    };
}
