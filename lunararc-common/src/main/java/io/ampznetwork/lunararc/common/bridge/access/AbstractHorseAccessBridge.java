package io.ampznetwork.lunararc.common.bridge.access;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Normal runtime bridge implemented by the corresponding Mixin accessor/invoker. */
public interface AbstractHorseAccessBridge {
    SimpleContainer lunararc$getInventory();
    Container lunararc$getBodyArmorAccess();
    void lunararc$recreateInventory();
}
