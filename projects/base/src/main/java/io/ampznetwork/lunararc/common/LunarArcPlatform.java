package io.ampznetwork.lunararc.common;

import net.minecraft.server.MinecraftServer;
import org.bukkit.craftbukkit.v1_21_R1.CraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central registry following the arclight-common pattern.
 *
 * Each platform module (lunararc-neoforge, lunararc-fabric, …) registers its
 * PlatformBridge here during mod initialisation.  The common mixins then use
 * the bridge for two critical operations:
 *
 *  1. Server creation  – createCraftServer() returns the platform-specific
 *     CraftServer subclass instead of the bare CraftServer stub.
 *  2. Class space unification – getModClassLoader() exposes the mod loader's
 *     ClassLoader so PluginClassLoader can delegate to it, letting plugins
 *     reference mod classes and vice-versa (the core fix for "plugins load but
 *     don't function in-game").
 */
public final class LunarArcPlatform {

    private static final Logger LOGGER = LoggerFactory.getLogger("LunarArc");

    private static PlatformBridge platformBridge;
    private static CraftServer server;

    public static String LATEST_VERSION = null;
    public static String UPDATE_URL = null;

    private LunarArcPlatform() {}

    // -------------------------------------------------------------------------
    // Bridge registration (called by each platform entry-point)
    // -------------------------------------------------------------------------

    public static void registerBridge(PlatformBridge bridge) {
        if (platformBridge != null) {
            LOGGER.warn("Overwriting existing platform bridge {} with {}",
                    platformBridge.getPlatformName(), bridge.getPlatformName());
        }
        platformBridge = bridge;
        LOGGER.info("[LunarArc] Registered platform bridge: {}", bridge.getPlatformName());
        bridge.initialize();
    }

    public static PlatformBridge getPlatformBridge() {
        return platformBridge;
    }

    // -------------------------------------------------------------------------
    // Class-space helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the mod loader's ClassLoader so PluginClassLoader can delegate to
     * it.  If no bridge is registered (e.g. in tests) returns null.
     */
    public static ClassLoader getModClassLoader() {
        return platformBridge != null ? platformBridge.getModClassLoader() : null;
    }

    // -------------------------------------------------------------------------
    // CraftServer factory
    // -------------------------------------------------------------------------

    /**
     * Creates the CraftServer implementation appropriate for the active platform.
     * Falls back to the plain CraftServer stub when no bridge is registered.
     */
    public static CraftServer createCraftServer(MinecraftServer minecraftServer) {
        if (platformBridge != null) {
            return platformBridge.createCraftServer(minecraftServer);
        }
        LOGGER.warn("No platform bridge registered — falling back to plain CraftServer");
        return new CraftServer(minecraftServer, minecraftServer.getPlayerList());
    }

    // -------------------------------------------------------------------------
    // Server accessor
    // -------------------------------------------------------------------------

    public static void setServer(CraftServer server) {
        LunarArcPlatform.server = server;
    }

    public static CraftServer getServer() {
        return server;
    }
}
