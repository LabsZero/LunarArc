package io.lunararcdevs.lunararc.common.mixin.core.world.block;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.lunararcdevs.lunararc.common.event.LunarArcBlockCapture;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BoneMealItem.class)
public abstract class BoneMealItemMixin {

    @WrapMethod(method = "useOn", require = 0)
    private InteractionResult lunararc$useOn(UseOnContext context, Operation<InteractionResult> original) {
        LunarArcBlockCapture.boneMeal(context.getPlayer());
        try {
            InteractionResult[] result = new InteractionResult[1];
            boolean applied = LunarArcBlockCapture.fertilize(context.getLevel(), context.getClickedPos(), context.getItemInHand(), () -> {
                result[0] = original.call(context);
                return result[0].consumesAction();
            });
            return result[0] != null && (applied || !result[0].consumesAction()) ? result[0] : InteractionResult.FAIL;
        } finally {
            LunarArcBlockCapture.boneMeal(null);
        }
    }
}
