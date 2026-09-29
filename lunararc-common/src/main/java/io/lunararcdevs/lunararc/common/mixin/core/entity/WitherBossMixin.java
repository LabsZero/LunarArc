package io.lunararcdevs.lunararc.common.mixin.core.entity;

import net.minecraft.world.entity.boss.wither.WitherBoss;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WitherBoss.class)
public abstract class WitherBossMixin {
    @Unique private boolean lunararc$canPortal = false;

    public void setCanTravelThroughPortals(boolean canPortal) {
        this.lunararc$canPortal = canPortal;
    }

    @Inject(method = "canUsePortal", at = @At("HEAD"), cancellable = true)
    private void lunararc$portal(boolean allowPassengers, CallbackInfoReturnable<Boolean> cir) {
        if (this.lunararc$canPortal) cir.setReturnValue(true);
    }
}
