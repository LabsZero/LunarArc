package io.lunararcdevs.lunararc.common.mixin.core.entity;

import io.lunararcdevs.lunararc.common.bridge.entity.SkeletonBridge;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import org.spongepowered.asm.mixin.Mixin;

/** Skeleton freeze conversion state lives in the skeleton's {@code freezingTracker} (made accessible by the access widener). */
@Mixin(Skeleton.class)
public abstract class SkeletonMixin implements SkeletonBridge {
    @Override public int lunararc$getConversionTime() { return ((Skeleton) (Object) this).freezingTracker.conversionTime; }
    @Override public int lunararc$getInPowderSnowTime() { return ((Skeleton) (Object) this).freezingTracker.afflictionTime; }
    @Override public void lunararc$startFreezeConversion(int ticks) { ((Skeleton) (Object) this).startFreezeConversion(ticks); }
    @Override public void lunararc$stopFreezeConversion() {
        Skeleton self = (Skeleton) (Object) this;
        self.freezingTracker.conversionTime = -1;
        self.freezingTracker.setConverting(false);
    }
}
