package io.lunararcdevs.lunararc.common.mixin.core.server;

import io.lunararcdevs.lunararc.common.event.LunarArcMoreEvents;
import net.minecraft.network.protocol.game.ServerboundPickItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class PickItemEventMixin {
    @Shadow public ServerPlayer player;

    @Inject(method = "handlePickItem", at = @At("HEAD"), cancellable = true, require = 0)
    private void lunararc$pick(ServerboundPickItemPacket packet, CallbackInfo ci) {
        if (!LunarArcMoreEvents.pickItem(this.player, packet.getSlot())) ci.cancel();
    }
}
