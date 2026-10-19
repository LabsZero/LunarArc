package io.lunararcdevs.lunararc.common.mixin.core.events;

import com.destroystokyo.paper.event.entity.EndermanEscapeEvent;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.lunararcdevs.lunararc.common.bridge.EntityBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.monster.Enderman;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Paper's EndermanEscapeEvent: every teleport an enderman makes to get away can be vetoed. */
@Mixin(Enderman.class)
public abstract class EndermanEscapeEventMixin {
    @WrapOperation(method = "customServerAiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/Enderman;teleport()Z"))
    private boolean lunararc$runaway(Enderman self, Operation<Boolean> original) {
        return lunararc$escape(EndermanEscapeEvent.Reason.RUNAWAY) && original.call(self);
    }

    @WrapOperation(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/Enderman;teleport()Z"))
    private boolean lunararc$hurt(Enderman self, Operation<Boolean> original, ServerLevel level, DamageSource source, float amount) {
        EndermanEscapeEvent.Reason reason = source.is(DamageTypeTags.IS_DROWNING) ? EndermanEscapeEvent.Reason.DROWN
                : source.is(DamageTypeTags.IS_PROJECTILE) ? EndermanEscapeEvent.Reason.CRITICAL_HIT : EndermanEscapeEvent.Reason.INDIRECT;
        return lunararc$escape(reason) && original.call(self);
    }

    @org.spongepowered.asm.mixin.Unique
    private boolean lunararc$escape(EndermanEscapeEvent.Reason reason) {
        return new EndermanEscapeEvent((org.bukkit.craftbukkit.entity.CraftEnderman) ((EntityBridge) this).lunararc$getBukkitEntity(), reason).callEvent();
    }
}
