package io.ampznetwork.lunararc.common;

import net.minecraft.server.MinecraftServer;
import org.bukkit.craftbukkit.v1_21_R1.CraftServer;

/**
 * Bridges the common (lunararc-common) module with each platform implementation.
 *
 * Following the arclight pattern:
 *  - Mixins live in io.ampznetwork.lunararc.common.mixin
 *  - Bridge interfaces (like this one) live in io.ampznetwork.lunararc.common.bridge
 *    and are implemented by the mixins / platform layer
 *  - Platform-specific code (lunararc-neoforge, lunararc-forge, …) implements this
 *    interface and registers itself via LunarArcPlatform.registerBridge()
 */
public interface PlatformBridge {

    /** Human-readable platform name shown in logs and /version output. */
    String getPlatformName();

    /** Called once during mod initialisation, before any server lifecycle. */
    void initialize();

    /** Called when the Minecraft server begins starting. */
    void onServerStarting();

    /** Called when the Minecraft server is stopping. */
    void onServerStopping();

    /**
     * Returns the class loader owned by the mod loader for this platform.
     *
     * Providing this loader to PluginClassLoader as a delegation target is the
     * key mechanism that lets Bukkit plugins reference mod classes at runtime —
     * mirroring how Arclight unifies the mod and plugin class spaces.
     */
    ClassLoader getModClassLoader();

    /**
     * Factory method that creates the platform-specific CraftServer subclass.
     *
     * The common MinecraftServerMixin calls this instead of hard-coding
     * {@code new CraftServer(…)}, allowing each platform to return its own
     * implementation (NeoForgeServer, FabricServer, ForgeServer, QuiltServer).
     */
    CraftServer createCraftServer(MinecraftServer server);
}
