package io.ampznetwork.lunararc.common.bridge.access;

import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import javax.annotation.Nullable;

/** Normal runtime bridge implemented by the corresponding Mixin accessor/invoker. */
public interface MerchantContainerAccessBridge {
    int lunararc$getSelectionHint();
    @Nullable MerchantOffer lunararc$getActiveOffer();
}
