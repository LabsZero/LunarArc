package io.lunararcdevs.lunararc.common.mixin.core.item;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.lunararcdevs.lunararc.common.event.LunarArcPaperEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.EndCrystalItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EndCrystalItem.class)
public abstract class EntityPlaceItemsMixin {

    @WrapOperation(method = "useOn", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean lunararc$place(Level level, Entity entity, Operation<Boolean> original, @Local(argsOnly = true) UseOnContext context) {
        return !LunarArcPaperEvents.placeEntity(entity, context.getPlayer(), context.getClickedPos(), context.getClickedFace(), context.getHand())
                && original.call(level, entity);
    }
}
