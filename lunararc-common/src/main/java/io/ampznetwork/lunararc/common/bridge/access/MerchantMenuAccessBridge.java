package io.ampznetwork.lunararc.common.bridge.access;

import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.Merchant;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Normal runtime bridge implemented by the corresponding Mixin accessor/invoker. */
public interface MerchantMenuAccessBridge {
    MerchantContainer lunararc$getTradeContainer();
    Merchant lunararc$getTrader();
}
