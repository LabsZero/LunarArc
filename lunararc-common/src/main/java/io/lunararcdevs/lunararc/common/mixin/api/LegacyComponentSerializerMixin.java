package io.lunararcdevs.lunararc.common.mixin.api;

import io.papermc.paper.adventure.providers.LegacyComponentSerializerProviderImpl;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = LegacyComponentSerializer.class, remap = false)
public interface LegacyComponentSerializerMixin {
    @SuppressWarnings("overwrite")
    @Overwrite
    static LegacyComponentSerializer legacySection() {
        return new LegacyComponentSerializerProviderImpl().legacySection();
    }

    @SuppressWarnings("overwrite")
    @Overwrite
    static LegacyComponentSerializer legacyAmpersand() {
        return new LegacyComponentSerializerProviderImpl().legacyAmpersand();
    }
}
