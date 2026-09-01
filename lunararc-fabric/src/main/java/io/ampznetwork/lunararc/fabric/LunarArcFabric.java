package io.ampznetwork.lunararc.fabric;

import io.ampznetwork.lunararc.common.mod.server.LunarArcServer;
import io.ampznetwork.lunararc.fabric.event.FabricBlockBreakEvents;
import io.ampznetwork.lunararc.fabric.server.FabricServerLifecycle;
import io.ampznetwork.lunararc.fabric.network.FabricChannelRegistration;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class LunarArcFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        io.ampznetwork.lunararc.common.LunarArcClientSideGuard.requireDedicatedServer(
                "Fabric", FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT);
        LunarArcServer.installPlatform("Fabric", LunarArcFabric.class.getClassLoader());
        FabricServerLifecycle.register();
        FabricChannelRegistration.register();
        FabricBlockBreakEvents.register();
    }
}
