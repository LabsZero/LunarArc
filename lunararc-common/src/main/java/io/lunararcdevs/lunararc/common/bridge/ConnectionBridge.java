package io.lunararcdevs.lunararc.common.bridge;

import net.minecraft.server.level.ServerPlayer;

public interface ConnectionBridge {
    int lunararc$getProtocolVersion();
    void lunararc$setProtocolVersion(int protocolVersion);

    String lunararc$getHostname();
    void lunararc$setHostname(String hostname);

    java.net.SocketAddress lunararc$getRawAddress();
    java.net.SocketAddress lunararc$getHAProxyAddress();
    void lunararc$setHAProxyAddress(java.net.SocketAddress address);

    java.util.UUID lunararc$getSpoofedUuid();
    void lunararc$setSpoofedUuid(java.util.UUID uuid);

    com.mojang.authlib.properties.Property[] lunararc$getSpoofedProfile();
    void lunararc$setSpoofedProfile(com.mojang.authlib.properties.Property[] profile);

    ServerPlayer lunararc$getLoginPlayer();
    void lunararc$setLoginPlayer(ServerPlayer player);
    io.netty.channel.Channel lunararc$getChannel();
}
