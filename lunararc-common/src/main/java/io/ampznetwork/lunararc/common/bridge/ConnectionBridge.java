package io.ampznetwork.lunararc.common.bridge;

import com.mojang.authlib.properties.Property;
import net.minecraft.server.level.ServerPlayer;

import java.net.SocketAddress;
import java.util.UUID;

public interface ConnectionBridge {
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

    // Used by PaperNmsAccessBridge — exposes the underlying Netty channel from
    // net.minecraft.network.Connection, which stores it as a non-public field.
    io.netty.channel.Channel lunararc$getChannel();
}
