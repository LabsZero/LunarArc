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

@Mixin(Pig.class)
public abstract class MobEvents_PigEventsMixin {
    @Unique private boolean lunararc$zapCancelled;

    @WrapOperation(method = "thunderHit", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean lunararc$zap(ServerLevel level, Entity piglin, Operation<Boolean> original, @Local(argsOnly = true) LightningBolt bolt) {
        this.lunararc$zapCancelled = !LunarArcMoreEvents.pigZap((Pig) (Object) this, bolt, piglin);
        return !this.lunararc$zapCancelled && original.call(level, piglin);
    }

    @WrapOperation(method = "thunderHit", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/Pig;discard()V"))
    private void lunararc$keep(Pig pig, Operation<Void> original) {
        boolean cancelled = this.lunararc$zapCancelled;
        this.lunararc$zapCancelled = false;
        if (!cancelled) original.call(pig);
    }
}
