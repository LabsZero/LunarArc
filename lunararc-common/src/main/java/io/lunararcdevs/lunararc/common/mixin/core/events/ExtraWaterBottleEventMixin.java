package io.lunararcdevs.lunararc.common.mixin.core.events;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.lunararcdevs.lunararc.common.event.LunarArcPotionEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** WaterBottleSplashEvent: wraps the water branch of onHit, which is where vanilla calls onHitAsWater. */
@Mixin(AbstractThrownPotion.class)
public abstract class ExtraWaterBottleEventMixin {
    @WrapOperation(method = "onHit", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/throwableitemprojectile/AbstractThrownPotion;affectEntitiesAround(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/alchemy/PotionContents;)V"))
    private void lunararc$waterSplash(AbstractThrownPotion potion, ServerLevel level, net.minecraft.world.item.alchemy.PotionContents contents,
            Operation<Void> original, HitResult hit) {
        // 26.x folds the water-bottle splash into affectEntitiesAround, selected by the potion's water-related tags
        if ((contents.is(net.minecraft.tags.PotionTags.HURTS_WATER_SENSITIVE_ENTITIES) || contents.is(net.minecraft.tags.PotionTags.EXTINGUISHES_ENTITIES))
                && LunarArcPotionEvents.waterSplash(potion, level, hit)) {
            return;
        }
        original.call(potion, level, contents);
    }
}
