package io.ampznetwork.lunararc.common.mixin.core.entity;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractHorse.class)
public interface AbstractHorseAccessor extends io.ampznetwork.lunararc.common.bridge.access.AbstractHorseAccessBridge {
    @Accessor("inventory") SimpleContainer lunararc$getInventory();
    @Accessor("bodyArmorAccess") Container lunararc$getBodyArmorAccess();
    @Invoker("createInventory") void lunararc$recreateInventory();
}
