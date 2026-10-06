package io.papermc.paper.registry;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public final class PaperRegistries {
    private PaperRegistries() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> ResourceKey registryToNms(RegistryKey<T> key) {
        return ResourceKey.createRegistryKey(ResourceLocation.parse(key.key().asString()));
    }
}
