package io.ampznetwork.lunararc.common.bridge.access;

import net.minecraft.world.entity.ItemBasedSteering;
import net.minecraft.world.entity.animal.Pig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Normal runtime bridge implemented by the corresponding Mixin accessor/invoker. */
public interface PigAccessBridge {
    ItemBasedSteering lunararc$getSteering();
}
