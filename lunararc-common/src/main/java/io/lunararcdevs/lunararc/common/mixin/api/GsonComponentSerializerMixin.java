package io.lunararcdevs.lunararc.common.mixin.api;

import io.papermc.paper.adventure.providers.GsonComponentSerializerProviderImpl;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/** Same reasoning as LegacyComponentSerializerMixin. */
@Mixin(value = GsonComponentSerializer.class, remap = false)
public interface GsonComponentSerializerMixin {
    @Overwrite
    static GsonComponentSerializer gson() {
        return new GsonComponentSerializerProviderImpl().gson();
    }

    @Overwrite
    static GsonComponentSerializer colorDownsamplingGson() {
        return new GsonComponentSerializerProviderImpl().gsonLegacy();
    }
}
