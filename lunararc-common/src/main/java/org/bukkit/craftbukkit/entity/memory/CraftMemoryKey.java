package org.bukkit.craftbukkit.entity.memory;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import org.bukkit.entity.memory.MemoryKey;

public final class CraftMemoryKey {
    private CraftMemoryKey() {}

    @SuppressWarnings("unchecked")
    public static <T, U> MemoryModuleType<U> bukkitToMinecraft(MemoryKey<T> key) {
        if (key == null) return null;
        ResourceLocation id = ResourceLocation.parse(key.getKey().toString());
        return (MemoryModuleType<U>) BuiltInRegistries.MEMORY_MODULE_TYPE.getOptional(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown memory key: " + key.getKey()));
    }
}
