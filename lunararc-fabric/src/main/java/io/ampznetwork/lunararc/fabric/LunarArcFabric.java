package io.ampznetwork.lunararc.fabric;

import io.ampznetwork.lunararc.common.mod.server.LunarArcServer;
import io.ampznetwork.lunararc.fabric.event.FabricBlockBreakEvents;
import io.ampznetwork.lunararc.fabric.server.FabricServerLifecycle;
import io.ampznetwork.lunararc.fabric.network.FabricChannelRegistration;
import net.fabricmc.api.ModInitializer;

public final class LunarArcFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        LunarArcServer.installPlatform("Fabric", LunarArcFabric.class.getClassLoader());
        FabricServerLifecycle.register();
        FabricChannelRegistration.register();
        FabricBlockBreakEvents.register();
    }
}
