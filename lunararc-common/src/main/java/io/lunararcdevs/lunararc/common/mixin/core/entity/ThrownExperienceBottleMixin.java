package io.lunararcdevs.lunararc.common.mixin.core.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.lunararcdevs.lunararc.common.event.LunarArcPaperEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.ThrownExperienceBottle;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.bukkit.event.entity.ExpBottleEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ThrownExperienceBottle.class)
public abstract class ThrownExperienceBottleMixin {

    @Unique private ExpBottleEvent lunararc$event;

    @WrapOperation(method = "onHit", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;levelEvent(ILnet/minecraft/core/BlockPos;I)V"))
    private void lunararc$effect(Level level, int type, BlockPos pos, int data, Operation<Void> original,
            @Local(argsOnly = true) HitResult hit) {
        this.lunararc$event = LunarArcPaperEvents.expBottle((ThrownExperienceBottle) (Object) this, hit);
        if (this.lunararc$event == null || this.lunararc$event.getShowEffect()) original.call(level, type, pos, data);
    }

    @WrapOperation(method = "onHit", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ExperienceOrb;award(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;I)V"))
    private void lunararc$award(ServerLevel level, Vec3 pos, int amount, Operation<Void> original) {
        ExpBottleEvent event = this.lunararc$event;
        this.lunararc$event = null;
        original.call(level, pos, event == null ? amount : event.getExperience());
    }
}
