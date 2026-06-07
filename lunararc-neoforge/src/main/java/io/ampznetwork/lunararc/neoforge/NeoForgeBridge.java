package io.ampznetwork.lunararc.neoforge;

import io.ampznetwork.lunararc.common.PlatformBridge;
import io.ampznetwork.lunararc.neoforge.server.NeoForgeServer;
import net.minecraft.server.MinecraftServer;
import org.bukkit.craftbukkit.v1_21_R1.CraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NeoForgeBridge implements PlatformBridge {

    private static final Logger LOGGER = LoggerFactory.getLogger("LunarArc-NeoForge");

    @Override
    public String getPlatformName() {
        return "NeoForge";
    }

    @Override
    public void initialize() {
        LOGGER.info("[LunarArc] NeoForge platform bridge initialized");
    }

    @Override
    public void onServerStarting() {}

    @Override
    public void onServerStopping() {}

    /**
     * Returns the NeoForge module class loader.
     * This is the same loader the mod loader uses for this mod, so any class
     * visible to NeoForge mods is also reachable from Bukkit plugins via
     * PluginClassLoader's mod-delegation step.
     */
    @Override
    public ClassLoader getModClassLoader() {
        return NeoForgeBridge.class.getClassLoader();
    }

    @Override
    public CraftServer createCraftServer(MinecraftServer server) {
        return new NeoForgeServer(server);
    }
}
