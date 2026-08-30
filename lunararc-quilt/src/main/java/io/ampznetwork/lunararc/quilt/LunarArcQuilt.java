package io.ampznetwork.lunararc.quilt;

import io.ampznetwork.lunararc.common.mod.server.LunarArcServer;
import io.ampznetwork.lunararc.quilt.event.QuiltBlockBreakEvents;
import io.ampznetwork.lunararc.quilt.network.QuiltChannelRegistration;
import io.ampznetwork.lunararc.quilt.server.QuiltServerLifecycle;
import net.fabricmc.api.ModInitializer;

public final class LunarArcQuilt implements ModInitializer {
    @Override
    public void onInitialize() {
        LunarArcServer.installPlatform("Quilt", LunarArcQuilt.class.getClassLoader());
        QuiltServerLifecycle.register();
        QuiltChannelRegistration.register();
        QuiltBlockBreakEvents.register();
    }
}
