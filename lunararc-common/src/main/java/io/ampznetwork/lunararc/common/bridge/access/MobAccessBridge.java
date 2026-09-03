package io.ampznetwork.lunararc.common.bridge.access;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Normal runtime bridge implemented by the corresponding Mixin accessor/invoker. */
public interface MobAccessBridge {
    void lunararc$setPersistenceRequired(boolean value);
    float lunararc$getBodyArmorDropChance();
    void lunararc$setBodyArmorDropChance(float value);
    SoundEvent lunararc$invokeGetAmbientSound();
}
