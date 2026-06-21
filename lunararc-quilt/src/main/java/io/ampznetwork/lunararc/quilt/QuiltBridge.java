package io.ampznetwork.lunararc.quilt;

import io.ampznetwork.lunararc.common.PlatformBridge;
import net.minecraft.server.MinecraftServer;
import org.bukkit.craftbukkit.v1_21_R1.CraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuiltBridge implements PlatformBridge {

    private static final Logger LOGGER = LoggerFactory.getLogger("LunarArc-Quilt");

    @Override
    public String getPlatformName() {
        return "Quilt";
    }

    @Override
    public void initialize() {
        LOGGER.info("[LunarArc] Quilt platform bridge initialized");
    }

    @Override
    public void onServerStarting() {}

    @Override
    public void onServerStopping() {}

    @Override
    public ClassLoader getModClassLoader() {
        return QuiltBridge.class.getClassLoader();
    }

    @Override
    public CraftServer createCraftServer(MinecraftServer server) {
        return new CraftServer(server, server.getPlayerList());
    }
}
