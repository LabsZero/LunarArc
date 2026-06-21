package io.ampznetwork.lunararc.fabric;

import io.ampznetwork.lunararc.common.PlatformBridge;
import net.minecraft.server.MinecraftServer;
import org.bukkit.craftbukkit.v1_21_R1.CraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FabricBridge implements PlatformBridge {

    private static final Logger LOGGER = LoggerFactory.getLogger("LunarArc-Fabric");

    @Override
    public String getPlatformName() {
        return "Fabric";
    }

    @Override
    public void initialize() {
        LOGGER.info("[LunarArc] Fabric platform bridge initialized");
    }

    @Override
    public void onServerStarting() {}

    @Override
    public void onServerStopping() {}

    @Override
    public ClassLoader getModClassLoader() {
        return FabricBridge.class.getClassLoader();
    }

    @Override
    public CraftServer createCraftServer(MinecraftServer server) {
        return new CraftServer(server, server.getPlayerList());
    }
}
