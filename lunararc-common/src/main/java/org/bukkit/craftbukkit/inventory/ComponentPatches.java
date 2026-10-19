package org.bukkit.craftbukkit.inventory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;

/** Presents a {@link DataComponentPatch} as the entry map older code expects (empty Optional marks a removed component). */
public final class ComponentPatches {
    private ComponentPatches() {
    }

    public static Map<DataComponentType<?>, Optional<?>> entries(final DataComponentPatch patch) {
        final Map<DataComponentType<?>, Optional<?>> entries = new LinkedHashMap<>();
        final DataComponentPatch.SplitResult split = patch.split();
        for (final TypedDataComponent<?> component : split.added()) {
            entries.put(component.type(), Optional.of(component.value()));
        }
        for (final DataComponentType<?> removed : split.removed()) {
            entries.put(removed, Optional.empty());
        }
        return entries;
    }
}
