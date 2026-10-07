package io.lunararcdevs.lunararc.common.mixin.core.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.lunararcdevs.lunararc.common.event.LunarArcPaperEvents;
import net.minecraft.world.entity.ambient.Bat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Bat.class)
public abstract class BatMixin {

    @WrapOperation(method = "customServerAiStep", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ambient/Bat;setResting(Z)V"))
    private void lunararc$toggleSleep(Bat bat, boolean resting, Operation<Void> original) {
        if (LunarArcPaperEvents.batToggle(bat, !resting)) original.call(bat, resting);
    }
}
