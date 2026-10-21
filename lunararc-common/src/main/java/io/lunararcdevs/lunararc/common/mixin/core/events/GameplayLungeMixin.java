package io.lunararcdevs.lunararc.common.mixin.core.events;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.lunararcdevs.lunararc.common.event.LunarArcGameplayEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** EntityLungeEvent: a plugin may change the power of, or cancel, the lunge effect of a piercing attack. */
@Mixin(Enchantment.class)
public abstract class GameplayLungeMixin {
    @WrapOperation(method = "lambda$doPostPiercingAttack$0", require = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/effects/EnchantmentEntityEffect;apply(Lnet/minecraft/server/level/ServerLevel;ILnet/minecraft/world/item/enchantment/EnchantedItemInUse;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;)V"))
    private static void lunararc$lunge(EnchantmentEntityEffect effect, ServerLevel level, int power, EnchantedItemInUse item, Entity user,
            Vec3 position, Operation<Void> original) {
        int adjusted = LunarArcGameplayEvents.lunge(user, power);
        if (adjusted < 0) return;
        original.call(effect, level, adjusted, item, user, position);
    }
}
