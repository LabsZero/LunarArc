package io.lunararcdevs.lunararc.common.mixin.core.entity;

import io.lunararcdevs.lunararc.common.bridge.entity.PiglinBridge;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PiglinAi.class)
public abstract class PiglinAiMixin {
    @Inject(method = "wantsToPickup", at = @At("HEAD"), cancellable = true)
    private static void lunararc$customItems(Piglin piglin, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        PiglinBridge bridge = (PiglinBridge) piglin;
        if ((bridge.lunararc$getAllowedBarterItems().contains(stack.getItem())
                || bridge.lunararc$getInterestItems().contains(stack.getItem()))
                && piglin.canPickUpLoot() && piglin.getOffhandItem().isEmpty()) {
            cir.setReturnValue(true);
        }
    }
}
