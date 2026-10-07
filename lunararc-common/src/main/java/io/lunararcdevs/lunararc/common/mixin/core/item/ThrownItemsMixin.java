package io.lunararcdevs.lunararc.common.mixin.core.item;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.lunararcdevs.lunararc.common.event.LunarArcPaperEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.ExperienceBottleItem;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.item.ThrowablePotionItem;
import net.minecraft.world.item.WindChargeItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({EggItem.class, SnowballItem.class, EnderpearlItem.class, ExperienceBottleItem.class, ThrowablePotionItem.class, WindChargeItem.class})
public abstract class ThrownItemsMixin {

    @WrapOperation(method = "use", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean lunararc$launch(Level level, Entity projectile, Operation<Boolean> original,
            @Local(argsOnly = true) Player player, @Local(argsOnly = true) InteractionHand hand) {
        return !LunarArcPaperEvents.launchProjectile(projectile, player.getItemInHand(hand)) && original.call(level, projectile);
    }
}
