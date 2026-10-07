package io.lunararcdevs.lunararc.common.mixin.core.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.lunararcdevs.lunararc.common.event.LunarArcPaperEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.projectile.ThrownEgg;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ThrownEgg.class)
public abstract class ThrownEggMixin {

    @Unique private int lunararc$hatches = 1;

    @WrapOperation(method = "onHit", require = 0,
            at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/util/RandomSource;nextInt(I)I"))
    private int lunararc$hatch(RandomSource random, int bound, Operation<Integer> original) {
        boolean hatching = original.call(random, bound) == 0;
        int[] result = LunarArcPaperEvents.eggHatch((ThrownEgg) (Object) this, hatching, random.nextInt(32) == 0 ? 4 : 1);
        this.lunararc$hatches = result[1];
        return result[0] == 1 ? 0 : 1;
    }

    @WrapOperation(method = "onHit", require = 0,
            at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/util/RandomSource;nextInt(I)I"))
    private int lunararc$count(RandomSource random, int bound, Operation<Integer> original) {
        original.call(random, bound);
        return this.lunararc$hatches >= 4 ? 0 : 1;
    }
}
