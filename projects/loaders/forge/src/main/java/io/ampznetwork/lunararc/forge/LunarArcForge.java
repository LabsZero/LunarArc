package io.ampznetwork.lunararc.forge;

import io.ampznetwork.lunararc.common.LunarArcPlatform;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("lunararc")
public class LunarArcForge {

    public LunarArcForge() {
        LunarArcPlatform.registerBridge(new ForgeBridge());
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onCommonSetup);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {}
}
