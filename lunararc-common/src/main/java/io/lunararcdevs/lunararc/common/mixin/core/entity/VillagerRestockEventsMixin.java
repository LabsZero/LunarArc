package io.lunararcdevs.lunararc.common.mixin.core.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.lunararcdevs.lunararc.common.event.LunarArcPaperEvents;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
public abstract class VillagerRestockEventsMixin {

    @WrapOperation(method = {"restock", "catchUpDemand"}, require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/trading/MerchantOffer;resetUses()V"))
    private void lunararc$replenish(MerchantOffer offer, Operation<Void> original) {
        if (LunarArcPaperEvents.replenish((Villager) (Object) this, offer)) original.call(offer);
    }

    @Inject(method = "setVillagerData", at = @At("HEAD"), cancellable = true, require = 0)
    private void lunararc$career(VillagerData data, CallbackInfo ci) {
        if (!LunarArcPaperEvents.career((Villager) (Object) this, data)) ci.cancel();
    }
}
