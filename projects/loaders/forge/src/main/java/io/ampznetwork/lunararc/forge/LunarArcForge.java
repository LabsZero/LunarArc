package io.ampznetwork.lunararc.forge;

import io.ampznetwork.lunararc.common.LunarArcPlatform;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;

@Mod("lunararc")
public class LunarArcForge {

    public LunarArcForge(IEventBus modEventBus) {
        LunarArcPlatform.registerBridge(new ForgeBridge());
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarting);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStopping);
    }

    private void onServerStarting(ServerStartingEvent event) {
        LunarArcPlatform.getPlatformBridge().onServerStarting();
    }

    private void onServerStopping(ServerStoppingEvent event) {
        LunarArcPlatform.getPlatformBridge().onServerStopping();
    }
}
