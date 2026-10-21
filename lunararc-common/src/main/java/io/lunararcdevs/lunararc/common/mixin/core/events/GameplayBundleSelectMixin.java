package io.lunararcdevs.lunararc.common.mixin.core.events;

import io.lunararcdevs.lunararc.common.event.LunarArcGameplayEvents;
import net.minecraft.network.protocol.game.ServerboundSelectBundleItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** PlayerBundleItemSelectEvent: fired before the bundle's selected item changes. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class GameplayBundleSelectMixin {
    @Shadow public ServerPlayer player;

    @Inject(method = "handleBundleItemSelectedPacket", at = @At("HEAD"), require = 1)
    private void lunararc$bundleSelect(ServerboundSelectBundleItemPacket packet, CallbackInfo ci) {
        // the packet arrives on a network thread first and is re-run on the server thread; only that second run counts
        if (!this.player.level().getServer().isSameThread()) return;
        LunarArcGameplayEvents.bundleSelect(this.player, packet.slotId(), packet.selectedItemIndex());
    }
}
