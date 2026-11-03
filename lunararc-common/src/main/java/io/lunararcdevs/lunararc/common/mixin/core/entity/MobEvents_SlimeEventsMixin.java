package io.lunararcdevs.lunararc.common.mixin.core.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.lunararcdevs.lunararc.common.event.LunarArcMoreEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.monster.Slime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Slime.class)
public abstract class MobEvents_SlimeEventsMixin {
    @WrapOperation(method = "remove", require = 0,
            at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/util/RandomSource;nextInt(I)I"))
    private int lunararc$split(RandomSource random, int bound, Operation<Integer> original) {
        int count = 2 + original.call(random, bound);
        return LunarArcMoreEvents.slimeSplit((Slime) (Object) this, count) - 2;
    }
}
