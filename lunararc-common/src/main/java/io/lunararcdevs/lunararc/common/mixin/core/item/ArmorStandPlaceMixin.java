package io.lunararcdevs.lunararc.common.mixin.core.item;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.lunararcdevs.lunararc.common.event.LunarArcPaperEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ArmorStandItem;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ArmorStandItem.class)
public abstract class ArmorStandPlaceMixin {

    @WrapOperation(method = "useOn", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V"))
    private void lunararc$placeStand(ServerLevel level, Entity entity, Operation<Void> original, @Local(argsOnly = true) UseOnContext context) {
        if (!LunarArcPaperEvents.placeEntity(entity, context.getPlayer(), context.getClickedPos(), context.getClickedFace(), context.getHand())) {
            original.call(level, entity);
        }
    }
}
