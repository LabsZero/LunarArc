package io.lunararcdevs.lunararc.common.mixin.core.entity;

import io.lunararcdevs.lunararc.common.event.LunarArcContainerOwners;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractHorse.class)
public abstract class AbstractHorseInventoryOwnerMixin {
    @Shadow protected SimpleContainer inventory;

    @Inject(method = "createInventory", at = @At("TAIL"), require = 0)
    private void lunararc$registerOwner(CallbackInfo ci) {
        LunarArcContainerOwners.register(this.inventory, (AbstractHorse) (Object) this);
    }
}
