package io.ampznetwork.lunararc.common.bridge.access;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Normal runtime bridge implemented by the corresponding Mixin accessor/invoker. */
public interface EntityAccessBridge {
    void lunararc$setSharedFlag(int flag, boolean value);
}
