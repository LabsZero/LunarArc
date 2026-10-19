package io.lunararcdevs.lunararc.common.bridge.entity;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.item.trading.TradeSet;

/** Narrow access to protected/private AbstractVillager trade state. */
public interface AbstractVillagerBridge {
    void lunararc$resetOffers();
    void lunararc$replaceOffers(MerchantOffers offers);
    /** Adds the trades of a vanilla {@link TradeSet} (Minecraft 26.x trades are data driven). */
    void lunararc$addOffersFromTradeSet(MerchantOffers offers, ResourceKey<TradeSet> tradeSet);
}
