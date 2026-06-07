package io.ampznetwork.lunararc.quilt;

import io.ampznetwork.lunararc.common.LunarArcPlatform;
import net.fabricmc.api.ModInitializer;

public class LunarArcQuilt implements ModInitializer {

    @Override
    public void onInitialize() {
        LunarArcPlatform.registerBridge(new QuiltBridge());
    }
}
