package io.lunararcdevs.lunararc.common.mixin.core.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.lunararcdevs.lunararc.common.event.LunarArcMoreEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.Slime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Slime.class)
public abstract class MobEvents_SlimeEventsMixin {
    @WrapOperation(method = "remove", require = 0,
            at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/util/RandomSource;nextInt(I)I"))
    private int lunararc$split(RandomSource random, int bound, Operation<Integer> original) {
        int count = 2 + original.call(random, bound);
        return LunarArcMoreEvents.slimeSplit((Slime) (Object) this, count) - 2;
    }
}
