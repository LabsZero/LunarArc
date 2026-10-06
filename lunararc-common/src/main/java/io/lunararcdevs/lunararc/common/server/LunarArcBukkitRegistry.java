package io.lunararcdevs.lunararc.common.server;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Holder;
import net.kyori.adventure.key.Key;
import io.papermc.paper.registry.tag.TagKey;
import io.papermc.paper.registry.tag.Tag;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.RegistryKey;
import io.lunararcdevs.lunararc.common.LunarArcServerAccess;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class LunarArcBukkitRegistry<T extends Keyed> implements Registry<T> {
    private final Map<NamespacedKey, T> byKey;
    private final List<T> values;

    private LunarArcBukkitRegistry(Map<NamespacedKey, T> byKey, List<T> values) {
        this.byKey = byKey;
        this.values = values;
    }

    public static <T extends Keyed> Registry<T> forType(Class<T> type) {
        if (type == null) return empty();

        Map<NamespacedKey, T> byKey = new LinkedHashMap<>();
        List<T> values = new ArrayList<>();

        if (type.isEnum()) {
            T[] constants = type.getEnumConstants();
            if (constants != null) {
                for (T value : constants) add(value, byKey, values);
            }
        }

        return new LunarArcBukkitRegistry<>(
                Collections.unmodifiableMap(byKey),
                Collections.unmodifiableList(values));
    }

    public static <T extends Keyed> Registry<T> fromValues(java.util.Collection<T> source) {
        Map<NamespacedKey, T> byKey = new LinkedHashMap<>();
        List<T> values = new ArrayList<>();
        for (T value : source) {
            add(value, byKey, values);
        }
        return new LunarArcBukkitRegistry<>(
                Collections.unmodifiableMap(byKey),
                Collections.unmodifiableList(values));
    }

    public static <T extends Keyed> Registry<T> empty() {
        return new LunarArcBukkitRegistry<>(Collections.emptyMap(), Collections.emptyList());
    }


    public static <T extends Keyed> Registry<T> lazy(Supplier<Collection<T>> valuesSupplier) {
        return new LazyRegistry<>(valuesSupplier);
    }

    private static final class LazyRegistry<T extends Keyed> implements Registry<T> {
        private final Supplier<Collection<T>> valuesSupplier;
        private volatile Registry<T> delegate;

        LazyRegistry(Supplier<Collection<T>> valuesSupplier) {
            this.valuesSupplier = valuesSupplier;
        }

        private Registry<T> delegate() {
            Registry<T> current = delegate;
            if (current != null) return current;
            synchronized (this) {
                current = delegate;
                if (current == null) {
                    current = delegate = fromValues(valuesSupplier.get());
                }
            }
            return current;
        }

        @Override public @Nullable T get(@NotNull NamespacedKey key) { return delegate().get(key); }

        @Override
        public @NotNull T getOrThrow(@NotNull NamespacedKey key) {
            return delegate().getOrThrow(key);
        }

        @Override public @Nullable NamespacedKey getKey(@NotNull T value) { return delegate().getKey(value); }

        @Override public @NotNull Iterator<T> iterator() { return delegate().iterator(); }

        @Override public @NotNull Stream<T> stream() { return delegate().stream(); }
    }

    private static <T extends Keyed> void add(T value, Map<NamespacedKey, T> byKey, List<T> values) {
        if (value == null) return;
        try {
            NamespacedKey key = value.getKey();
            if (key == null) return;
            byKey.putIfAbsent(key, value);
            values.add(value);
        } catch (Throwable ignored) {
        }
    }

    @Override public @Nullable T get(@NotNull NamespacedKey key) { return byKey.get(key); }

    @Override
    public @NotNull T getOrThrow(@NotNull NamespacedKey key) {
        T value = get(key);
        if (value == null) throw new NoSuchElementException(key.toString());
        return value;
    }

    @Override
    public @Nullable NamespacedKey getKey(@NotNull T value) {
        try {
            return value.getKey();
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Override public @NotNull Iterator<T> iterator() { return values.iterator(); }
    @Override public @NotNull Stream<T> stream() { return values.stream(); }

    public static final class Tagged<T extends Keyed> implements Registry<T> {
        private final Registry<T> delegate;
        private final RegistryKey<T> registryKey;

        public Tagged(Registry<T> delegate, RegistryKey<T> registryKey) {
            this.delegate = delegate;
            this.registryKey = registryKey;
        }

        @Override
        public @Nullable T get(@NotNull NamespacedKey key) {
            return delegate.get(key);
        }

        @Override
        public @NotNull T getOrThrow(@NotNull NamespacedKey key) {
            return delegate.getOrThrow(key);
        }

        @Override
        public @NotNull Stream<T> stream() {
            return delegate.stream();
        }

        @Override
        public @NotNull Iterator<T> iterator() {
            return delegate.iterator();
        }

        @Override
        public @Nullable NamespacedKey getKey(@NotNull T value) {
            return delegate.getKey(value);
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private java.util.Optional<HolderSet.Named<Object>> nmsTag(TagKey<T> key) {
            net.minecraft.core.Registry<Object> nms = LunarArcServerAccess.getMinecraftServer().registryAccess()
                    .registryOrThrow((ResourceKey) io.papermc.paper.registry.PaperRegistries.registryToNms(registryKey));
            net.minecraft.tags.TagKey<Object> nmsKey = net.minecraft.tags.TagKey.create(nms.key(),
                    ResourceLocation.parse(key.key().asString()));
            return nms.getTag(nmsKey);
        }

        @Override
        public boolean hasTag(@NotNull TagKey<T> key) {
            return nmsTag(key).isPresent();
        }

        @Override
        public @NotNull Tag<T> getTag(@NotNull TagKey<T> key) {
            HolderSet.Named<Object> named = nmsTag(key)
                    .orElseThrow(() -> new java.util.NoSuchElementException("No tag " + key.key() + " in " + registryKey));
            List<TypedKey<T>> values = new ArrayList<>();
            for (Holder<Object> holder : named) {
                holder.unwrapKey().ifPresent(resourceKey ->
                        values.add(TypedKey.create(registryKey, Key.key(resourceKey.location().getNamespace(), resourceKey.location().getPath()))));
            }
            return new TagImpl<>(key, registryKey, List.copyOf(values));
        }

        private record TagImpl<T extends Keyed>(TagKey<T> tagKey, RegistryKey<T> registryKey, List<TypedKey<T>> keys) implements Tag<T> {
            @Override
            public @NotNull Collection<TypedKey<T>> values() {
                return keys;
            }

            @Override
            public int size() {
                return keys.size();
            }

            @Override
            public @NotNull Collection<T> resolve(@NotNull Registry<T> registry) {
                List<T> resolved = new ArrayList<>(keys.size());
                for (TypedKey<T> typed : keys) {
                    resolved.add(registry.getOrThrow(new NamespacedKey(typed.key().namespace(), typed.key().value())));
                }
                return resolved;
            }

            @Override
            public boolean contains(@NotNull TypedKey<T> key) {
                return keys.contains(key);
            }

            @Override
            public @NotNull Iterator<TypedKey<T>> iterator() {
                return keys.iterator();
            }
        }
    }
}
