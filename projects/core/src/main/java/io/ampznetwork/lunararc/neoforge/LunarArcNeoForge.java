package io.ampznetwork.lunararc.neoforge;

import io.ampznetwork.lunararc.common.LunarArcPlatform;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

@Mod("lunararc")
public class LunarArcNeoForge {

    public LunarArcNeoForge(IEventBus modBus) {
        // Register the bridge before any server lifecycle events so that
        // MinecraftServerMixin.lunararc$afterServerInit can call createCraftServer().
        LunarArcPlatform.registerBridge(new NeoForgeBridge());

        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
        NeoForge.EVENT_BUS.addListener(this::onServerStopping);
    }

    private void onServerStarting(ServerStartingEvent event) {
        LunarArcPlatform.getPlatformBridge().onServerStarting();
    }

    private void onServerStopping(ServerStoppingEvent event) {
        LunarArcPlatform.getPlatformBridge().onServerStopping();
    }
}
