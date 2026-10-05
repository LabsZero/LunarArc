package io.lunararcdevs.lunararc.common.server;

import io.lunararcdevs.lunararc.common.LunarArcServerAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.tag.Tag;
import io.papermc.paper.registry.tag.TagKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import net.kyori.adventure.key.Key;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class LunarArcTaggedRegistry<T extends Keyed> implements Registry<T> {
    private final Registry<T> delegate;
    private final RegistryKey<T> registryKey;

    public LunarArcTaggedRegistry(Registry<T> delegate, RegistryKey<T> registryKey) {
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
