package io.lunararcdevs.lunararc.common.mixin.api;

import io.papermc.paper.adventure.providers.PlainTextComponentSerializerProviderImpl;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/** Same reasoning as LegacyComponentSerializerMixin. */
@Mixin(value = PlainTextComponentSerializer.class, remap = false)
public interface PlainTextComponentSerializerMixin {
    @Overwrite
    static PlainTextComponentSerializer plainText() {
        return new PlainTextComponentSerializerProviderImpl().plainTextSimple();
    }
}
