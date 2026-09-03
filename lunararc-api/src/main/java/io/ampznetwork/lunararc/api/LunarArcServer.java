package io.ampznetwork.lunararc.api;

import org.bukkit.plugin.Plugin;

import java.util.Objects;

public final class LunarArcServer {

    private static LunarArcServerApi server;

    private LunarArcServer() {
    }

    @Deprecated
    public static void init() {
    }

    public static LunarArcVersion getVersion() {
        return getServer().getVersion();
    }

    public static void registerModEvent(Plugin plugin, Object eventBus, Object target) {
        getServer().registerModEvent(plugin, eventBus, target);
    }

    public static LunarArcTickingTracker getTickingTracker() {
        return getServer().getTickingTracker();
    }

    public static LunarArcPlatform getPlatform() {
        return getServer().getPlatform();
    }

    private static LunarArcServerApi getServer() {
        return Objects.requireNonNull(server, "Server not set!");
    }

    public static void setServer(LunarArcServerApi server) {
        Objects.requireNonNull(server, "server");
        if (LunarArcServer.server != null) {
            throw new IllegalStateException("Server already set!");
        }
        LunarArcServer.server = server;
    }
}
