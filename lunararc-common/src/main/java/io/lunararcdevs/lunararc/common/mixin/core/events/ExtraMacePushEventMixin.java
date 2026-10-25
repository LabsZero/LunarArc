package io.lunararcdevs.lunararc.common.mixin.core.events;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.lunararcdevs.lunararc.common.event.LunarArcMobEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * EntityPushedByEntityAttackEvent for a mace smash. The attacker is held in a static while the knockback runs,
 * because the push itself happens in a lambda that is not given the attacker directly.
 */
@Mixin(MaceItem.class)
public abstract class ExtraMacePushEventMixin {
    @WrapMethod(method = "knockback", require = 0)
    private static void lunararc$context(Level level, Entity attacker, Entity target, Operation<Void> original) {
        Entity before = LunarArcMobEvents.pusher;
        LunarArcMobEvents.pusher = attacker;
        try {
            original.call(level, attacker, target);
        } finally {
            LunarArcMobEvents.pusher = before;
        }
    }

    @WrapOperation(method = "lambda$knockback$0", remap = false, require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;push(DDD)V"))
    private static void lunararc$push(net.minecraft.world.entity.LivingEntity target, double x, double y, double z, Operation<Void> original) {
        Vec3 push = LunarArcMobEvents.pushedByAttack(target, LunarArcMobEvents.pusher, x, y, z);
        if (push == null) return;
        original.call(target, push.x, push.y, push.z);
    }
}
