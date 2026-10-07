package io.lunararcdevs.lunararc.common.mixin.core.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.lunararcdevs.lunararc.common.event.LunarArcPaperEvents;
import io.papermc.paper.event.player.PlayerTradeEvent;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractVillager.class)
public abstract class VillagerTradeEventsMixin {

    @Unique private PlayerTradeEvent lunararc$tradeEvent;

    @Shadow public abstract Player getTradingPlayer();

    @Inject(method = "notifyTrade", at = @At("HEAD"), require = 0)
    private void lunararc$tradeStart(MerchantOffer offer, CallbackInfo ci) {
        this.lunararc$tradeEvent = LunarArcPaperEvents.trade((AbstractVillager) (Object) this, this.getTradingPlayer(), offer);
    }

    @Inject(method = "notifyTrade", at = @At("RETURN"), require = 0)
    private void lunararc$tradeEnd(MerchantOffer offer, CallbackInfo ci) {
        this.lunararc$tradeEvent = null;
    }

    @WrapOperation(method = "notifyTrade", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/trading/MerchantOffer;increaseUses()V"))
    private void lunararc$uses(MerchantOffer offer, Operation<Void> original) {
        if (this.lunararc$tradeEvent == null || this.lunararc$tradeEvent.willIncreaseTradeUses()) original.call(offer);
    }

    @WrapOperation(method = "notifyTrade", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/npc/AbstractVillager;rewardTradeXp(Lnet/minecraft/world/item/trading/MerchantOffer;)V"))
    private void lunararc$xp(AbstractVillager villager, MerchantOffer offer, Operation<Void> original) {
        if (this.lunararc$tradeEvent == null || this.lunararc$tradeEvent.isRewardingExp()) original.call(villager, offer);
    }

    @WrapOperation(method = "addOffersFromItemListings", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/trading/MerchantOffers;add(Ljava/lang/Object;)Z"))
    private boolean lunararc$acquire(MerchantOffers offers, Object offer, Operation<Boolean> original) {
        MerchantOffer acquired = LunarArcPaperEvents.acquire((AbstractVillager) (Object) this, (MerchantOffer) offer);
        return acquired != null && original.call(offers, acquired);
    }
}
