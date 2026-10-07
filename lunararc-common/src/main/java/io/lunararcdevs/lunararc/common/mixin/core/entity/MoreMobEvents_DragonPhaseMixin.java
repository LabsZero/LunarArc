package io.lunararcdevs.lunararc.common.mixin.core.entity;

import io.lunararcdevs.lunararc.common.event.LunarArcMoreEvents;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhaseManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnderDragonPhaseManager.class)
public abstract class MoreMobEvents_DragonPhaseMixin {
    @Shadow @Final private EnderDragon dragon;
    @Shadow public abstract net.minecraft.world.entity.boss.enderdragon.phases.DragonPhaseInstance getCurrentPhase();

    @Inject(method = "setPhase", at = @At("HEAD"), cancellable = true, require = 0)
    private void lunararc$phase(EnderDragonPhase<?> phase, CallbackInfo ci) {
        if (this.getCurrentPhase() == null) return;
        if (!LunarArcMoreEvents.dragonPhase(this.dragon, this.getCurrentPhase().getPhase().getId(), phase.getId())) ci.cancel();
    }
}
