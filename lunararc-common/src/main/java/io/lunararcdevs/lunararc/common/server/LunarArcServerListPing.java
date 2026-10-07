package io.lunararcdevs.lunararc.common.server;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import com.destroystokyo.paper.network.StatusClient;
import com.mojang.authlib.GameProfile;
import io.lunararcdevs.lunararc.common.LunarArcServerAccess;
import io.lunararcdevs.lunararc.common.bridge.ConnectionBridge;
import io.lunararcdevs.lunararc.common.bridge.EntityBridge;
import io.papermc.paper.adventure.PaperAdventure;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.status.ClientboundStatusResponsePacket;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.MinecraftServer;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.util.CraftIconCache;
import org.bukkit.entity.Player;
import org.bukkit.event.server.ServerListPingEvent;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class LunarArcServerListPing {
    private LunarArcServerListPing() {}

    private static final class Client implements StatusClient {
        private final Connection connection;

        private Client(Connection connection) {
            this.connection = connection;
        }

        @Override
        public InetSocketAddress getAddress() {
            return this.connection.getRemoteAddress() instanceof InetSocketAddress address ? address : null;
        }

        @Override
        public int getProtocolVersion() {
            return ((ConnectionBridge) this.connection).lunararc$getProtocolVersion();
        }

        @Override
        public InetSocketAddress getVirtualHost() {
            String hostname = ((ConnectionBridge) this.connection).lunararc$getHostname();
            int split = hostname.lastIndexOf(':');
            if (split <= 0) return null;
            String host = hostname.substring(0, split);
            int marker = host.indexOf('\0');
            if (marker >= 0) host = host.substring(0, marker);
            if (host.endsWith(".")) host = host.substring(0, host.length() - 1);
            try {
                return InetSocketAddress.createUnresolved(host, Integer.parseInt(hostname.substring(split + 1)));
            } catch (NumberFormatException invalid) {
                return null;
            }
        }
    }

    private static final class Event extends PaperServerListPingEvent {
        private final MinecraftServer server;

        private Event(MinecraftServer server, CraftServer craft, Connection connection, ServerStatus status) {
            super(new Client(connection), PaperAdventure.asAdventure(status.description()),
                    status.players().map(ServerStatus.Players::online).orElse(server.getPlayerCount()),
                    status.players().map(ServerStatus.Players::max).orElse(server.getMaxPlayers()),
                    status.version().map(ServerStatus.Version::name).orElse(server.getServerVersion()),
                    status.version().map(ServerStatus.Version::protocol).orElse(-1), craft.getServerIcon());
            this.server = server;
            status.players().ifPresent(players -> {
                for (GameProfile profile : players.sample()) {
                    getListedPlayers().add(new ListedPlayerInfo(profile.getName(), profile.getId()));
                }
            });
        }

        @Override
        protected Object[] getOnlinePlayers() {
            return this.server.getPlayerList().getPlayers().toArray();
        }

        @Override
        protected Player getBukkitPlayer(Object player) {
            return (Player) ((EntityBridge) player).lunararc$getBukkitEntity();
        }
    }

    public static boolean process(MinecraftServer server, Connection connection) {
        if (ServerListPingEvent.getHandlerList().getRegisteredListeners().length == 0) return false;
        ServerStatus status = server.getStatus();
        if (status == null || status.version().isEmpty()) return false;
        CraftServer craft = LunarArcServerAccess.getCraftServer(server);
        Event event = new Event(server, craft, connection, status);
        craft.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            connection.disconnect((net.minecraft.network.chat.Component) null);
            return true;
        }

        Optional<ServerStatus.Players> players = Optional.empty();
        if (!event.shouldHidePlayers()) {
            List<GameProfile> sample = new ArrayList<>();
            for (PaperServerListPingEvent.ListedPlayerInfo info : event.getListedPlayers()) {
                sample.add(new GameProfile(info.id(), info.name()));
            }
            players = Optional.of(new ServerStatus.Players(event.getMaxPlayers(), event.getNumPlayers(), sample));
        }
        Optional<ServerStatus.Favicon> favicon = event.getServerIcon() instanceof CraftIconCache icon && icon.getValue() != null
                ? Optional.of(new ServerStatus.Favicon(icon.getValue())) : Optional.empty();
        connection.send(new ClientboundStatusResponsePacket(new ServerStatus(PaperAdventure.asVanilla(event.motd()), players,
                Optional.of(new ServerStatus.Version(event.getVersion(), event.getProtocolVersion())), favicon,
                server.enforceSecureProfile())));
        return true;
    }
}
