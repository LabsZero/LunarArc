package io.lunararcdevs.lunararc.common.mixin.core.events;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.lunararcdevs.lunararc.common.event.LunarArcMobEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.warden.SonicBoom;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** EntityPushedByEntityAttackEvent for a warden's sonic boom. */
@Mixin(SonicBoom.class)
public abstract class ExtraSonicBoomPushEventMixin {
    @WrapMethod(method = "tick", require = 0)
    private void lunararc$context(ServerLevel level, Warden warden, long gameTime, Operation<Void> original) {
        net.minecraft.world.entity.Entity before = LunarArcMobEvents.pusher;
        LunarArcMobEvents.pusher = warden;
        try {
            original.call(level, warden, gameTime);
        } finally {
            LunarArcMobEvents.pusher = before;
        }
    }

    @WrapOperation(method = {"lambda$tick$0", "lambda$tick$1", "lambda$tick$2"}, remap = false, require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;push(DDD)V"))
    private static void lunararc$push(LivingEntity target, double x, double y, double z, Operation<Void> original) {
        Vec3 push = LunarArcMobEvents.pushedByAttack(target, LunarArcMobEvents.pusher, x, y, z);
        if (push == null) return;
        original.call(target, push.x, push.y, push.z);
    }
}
