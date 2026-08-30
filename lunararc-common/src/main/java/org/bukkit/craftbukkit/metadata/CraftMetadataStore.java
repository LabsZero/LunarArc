package org.bukkit.craftbukkit.metadata;

import org.bukkit.metadata.MetadataStore;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;


public final class CraftMetadataStore<T> implements MetadataStore<T> {
    private final Map<T, Map<String, Map<Plugin, MetadataValue>>> values = new ConcurrentHashMap<>();

    @Override
    public void setMetadata(@NotNull T subject, @NotNull String metadataKey, @NotNull MetadataValue newMetadataValue) {
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(metadataKey, "metadataKey");
        Objects.requireNonNull(newMetadataValue, "newMetadataValue");
        Plugin owner = Objects.requireNonNull(newMetadataValue.getOwningPlugin(), "metadata value owner");
        values.computeIfAbsent(subject, ignored -> new ConcurrentHashMap<>())
                .computeIfAbsent(metadataKey, ignored -> Collections.synchronizedMap(new LinkedHashMap<>()))
                .put(owner, newMetadataValue);
    }

    @Override
    public @NotNull List<MetadataValue> getMetadata(@NotNull T subject, @NotNull String metadataKey) {
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(metadataKey, "metadataKey");
        Map<String, Map<Plugin, MetadataValue>> subjectValues = values.get(subject);
        if (subjectValues == null) return List.of();
        Map<Plugin, MetadataValue> keyValues = subjectValues.get(metadataKey);
        if (keyValues == null || keyValues.isEmpty()) return List.of();
        synchronized (keyValues) {
            return List.copyOf(new ArrayList<>(keyValues.values()));
        }
    }

    @Override
    public boolean hasMetadata(@NotNull T subject, @NotNull String metadataKey) {
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(metadataKey, "metadataKey");
        Map<String, Map<Plugin, MetadataValue>> subjectValues = values.get(subject);
        if (subjectValues == null) return false;
        Map<Plugin, MetadataValue> keyValues = subjectValues.get(metadataKey);
        return keyValues != null && !keyValues.isEmpty();
    }

    @Override
    public void removeMetadata(@NotNull T subject, @NotNull String metadataKey, @NotNull Plugin owningPlugin) {
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(metadataKey, "metadataKey");
        Objects.requireNonNull(owningPlugin, "owningPlugin");
        Map<String, Map<Plugin, MetadataValue>> subjectValues = values.get(subject);
        if (subjectValues == null) return;
        Map<Plugin, MetadataValue> keyValues = subjectValues.get(metadataKey);
        if (keyValues == null) return;
        keyValues.remove(owningPlugin);
        if (keyValues.isEmpty()) subjectValues.remove(metadataKey, keyValues);
        if (subjectValues.isEmpty()) values.remove(subject, subjectValues);
    }

    @Override
    public void invalidateAll(@NotNull Plugin owningPlugin) {
        Objects.requireNonNull(owningPlugin, "owningPlugin");
        for (Map.Entry<T, Map<String, Map<Plugin, MetadataValue>>> subjectEntry : values.entrySet()) {
            Map<String, Map<Plugin, MetadataValue>> subjectValues = subjectEntry.getValue();
            for (Map.Entry<String, Map<Plugin, MetadataValue>> keyEntry : subjectValues.entrySet()) {
                MetadataValue removed = keyEntry.getValue().remove(owningPlugin);
                if (removed != null) removed.invalidate();
                if (keyEntry.getValue().isEmpty()) subjectValues.remove(keyEntry.getKey(), keyEntry.getValue());
            }
            if (subjectValues.isEmpty()) values.remove(subjectEntry.getKey(), subjectValues);
        }
    }
}
