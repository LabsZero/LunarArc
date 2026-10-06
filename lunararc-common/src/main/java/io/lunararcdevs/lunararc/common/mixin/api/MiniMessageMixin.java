package io.lunararcdevs.lunararc.common.mixin.api;

import io.papermc.paper.adventure.providers.MiniMessageProviderImpl;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/** Same reasoning as LegacyComponentSerializerMixin. */
@Mixin(value = MiniMessage.class, remap = false)
public interface MiniMessageMixin {
    @SuppressWarnings("overwrite")
    @Overwrite
    static MiniMessage miniMessage() {
        return new MiniMessageProviderImpl().miniMessage();
    }
}
