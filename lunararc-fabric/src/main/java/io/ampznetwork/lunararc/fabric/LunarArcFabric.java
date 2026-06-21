package io.ampznetwork.lunararc.fabric;

import io.ampznetwork.lunararc.common.LunarArcPlatform;
import net.fabricmc.api.ModInitializer;

public class LunarArcFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        LunarArcPlatform.registerBridge(new FabricBridge());
    }
}
