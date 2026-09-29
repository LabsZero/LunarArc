package io.lunararcdevs.lunararc.common.mixin.api;

import io.papermc.paper.plugin.entrypoint.classloader.ClassloaderBytecodeModifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = ClassloaderBytecodeModifier.class, remap = false)
public interface ClassloaderBytecodeModifierMixin {
    /**
     * @author LunarArc
     * @reason Bypasses the fragile ServiceLoader-backed Provider field entirely.
     */
    @Overwrite
    static ClassloaderBytecodeModifier bytecodeModifier() {
        return io.lunararcdevs.lunararc.common.server.LunarArcClassloaderBytecodeModifierHolder.INSTANCE;
    }
}
