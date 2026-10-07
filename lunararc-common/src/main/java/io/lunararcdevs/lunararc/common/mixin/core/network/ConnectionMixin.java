package io.lunararcdevs.lunararc.common.mixin.core.network;

import com.mojang.authlib.properties.Property;
import io.lunararcdevs.lunararc.common.bridge.ConnectionBridge;
import io.netty.channel.Channel;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.PacketListener;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.SocketAddress;
import java.util.UUID;

@Mixin(Connection.class)
public abstract class ConnectionMixin implements ConnectionBridge {

    @Shadow public Channel channel;

    @Unique public Channel n;
    @Unique private String lunararc$hostname = "";
    @Unique private SocketAddress lunararc$rawAddress;
    @Unique private SocketAddress lunararc$haProxyAddress;
    @Unique private UUID lunararc$spoofedUuid;
    @Unique private Property[] lunararc$spoofedProfile;
    @Unique private ServerPlayer lunararc$loginPlayer;

    @Inject(method = "channelActive", at = @At("TAIL"))
    private void lunararc$channelActive(ChannelHandlerContext ctx, CallbackInfo ci) {
        this.n = this.channel;
        this.lunararc$rawAddress = this.channel.remoteAddress();
    }

    @Unique private int lunararc$protocolVersion = -1;

    @Override
    public int lunararc$getProtocolVersion() {
        return this.lunararc$protocolVersion;
    }

    @Override
    public void lunararc$setProtocolVersion(int protocolVersion) {
        this.lunararc$protocolVersion = protocolVersion;
    }

    @Override
    public String lunararc$getHostname() {
        return this.lunararc$hostname;
    }

    @Override
    public void lunararc$setHostname(String hostname) {
        this.lunararc$hostname = java.util.Objects.requireNonNull(hostname, "hostname");
    }

    @Override
    public SocketAddress lunararc$getRawAddress() {
        return this.lunararc$rawAddress;
    }

    @Override
    public SocketAddress lunararc$getHAProxyAddress() {
        return this.lunararc$haProxyAddress;
    }

    @Override
    public void lunararc$setHAProxyAddress(SocketAddress address) {
        this.lunararc$haProxyAddress = address;
    }

    @Override
    public UUID lunararc$getSpoofedUuid() {
        return this.lunararc$spoofedUuid;
    }

    @Override
    public void lunararc$setSpoofedUuid(UUID uuid) {
        this.lunararc$spoofedUuid = uuid;
    }

    @Override
    public Property[] lunararc$getSpoofedProfile() {
        return this.lunararc$spoofedProfile;
    }

    @Override
    public void lunararc$setSpoofedProfile(Property[] profile) {
        this.lunararc$spoofedProfile = profile;
    }

    @Override
    public ServerPlayer lunararc$getLoginPlayer() {
        return this.lunararc$loginPlayer;
    }

    @Override
    public void lunararc$setLoginPlayer(ServerPlayer player) {
        this.lunararc$loginPlayer = player;
    }

    @Override
    public Channel lunararc$getChannel() {
        return this.channel;
    }

    @WrapOperation(method = "exceptionCaught", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/network/Connection;send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V"))
    private void lunararc$skipUnregisteredDisconnect(Connection self, Packet<?> packet, PacketSendListener listener,
            Operation<Void> original) {
        PacketListener packetListener = self.getPacketListener();
        ConnectionProtocol protocol = packetListener == null ? null : packetListener.protocol();
        if (protocol == ConnectionProtocol.HANDSHAKING || protocol == ConnectionProtocol.STATUS) {
            self.disconnect(Component.translatable("disconnect.genericReason"));
            return;
        }
        original.call(self, packet, listener);
    }
}
