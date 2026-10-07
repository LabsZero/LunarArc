package io.lunararcdevs.lunararc.common.mixin.core.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.lunararcdevs.lunararc.common.event.LunarArcPaperEvents;
import net.minecraft.world.entity.monster.Strider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Strider.class)
public abstract class StriderMixin {

    @WrapOperation(method = "tick", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/Strider;setSuffocating(Z)V"))
    private void lunararc$temperature(Strider strider, boolean suffocating, Operation<Void> original) {
        if (suffocating == strider.isSuffocating() || LunarArcPaperEvents.striderToggle(strider, suffocating)) original.call(strider, suffocating);
    }
}
