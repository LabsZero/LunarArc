package io.lunararcdevs.lunararc.common.mixin.core.server;

import io.lunararcdevs.lunararc.common.bridge.ConnectionBridge;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.server.network.ServerHandshakePacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerHandshakePacketListenerImpl.class)
public abstract class ServerHandshakePacketListenerImplMixin {

    @Shadow @Final private Connection connection;

    @Inject(method = "handleIntention", at = @At("HEAD"), cancellable = true)
    private void lunararc$captureHostname(ClientIntentionPacket packet, CallbackInfo ci) {
        ((ConnectionBridge) this.connection).lunararc$setHostname(packet.hostName() + ":" + packet.port());
        ((ConnectionBridge) this.connection).lunararc$setProtocolVersion(packet.protocolVersion());
        if (packet.intention() == net.minecraft.network.protocol.handshake.ClientIntent.LOGIN) {
            net.kyori.adventure.text.Component failure = io.lunararcdevs.lunararc.common.event.LunarArcMoreEvents.handshake(packet.hostName());
            if (failure != null) {
                this.connection.disconnect(io.papermc.paper.adventure.PaperAdventure.asVanilla(failure));
                ci.cancel();
            }
        }
    }
}
