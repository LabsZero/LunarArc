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

    // Brain memory keys, under CraftBukkit's names. Both sides tolerate null, as CraftBukkit's do.
    public static <T, U> org.bukkit.entity.memory.MemoryKey<U> minecraftToBukkit(
            net.minecraft.world.entity.ai.memory.MemoryModuleType<T> minecraft) {
        if (minecraft == null) {
            return null;
        }

        net.minecraft.core.Registry<net.minecraft.world.entity.ai.memory.MemoryModuleType<?>> registry =
                org.bukkit.craftbukkit.CraftRegistry.getMinecraftRegistry(net.minecraft.core.registries.Registries.MEMORY_MODULE_TYPE);
        return org.bukkit.Registry.MEMORY_MODULE_TYPE.get(
                org.bukkit.craftbukkit.util.CraftNamespacedKey.fromMinecraft(registry.getResourceKey(minecraft).orElseThrow().location()));
    }

}
