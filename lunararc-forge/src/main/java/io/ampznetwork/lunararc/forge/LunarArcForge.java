package io.ampznetwork.lunararc.forge;

import io.ampznetwork.lunararc.common.LunarArcClientSideGuard;
import io.ampznetwork.lunararc.common.mod.server.LunarArcServer;
import io.ampznetwork.lunararc.forge.command.ForgeCommandHook;
import io.ampznetwork.lunararc.forge.server.ForgeServerLifecycle;
import io.ampznetwork.lunararc.forge.network.ForgeChannelRegistration;
import io.ampznetwork.lunararc.forge.event.ForgeBlockBreakEvents;
import io.ampznetwork.lunararc.forge.event.ForgeBlockPlaceEvents;
import io.ampznetwork.lunararc.forge.event.ForgeEntityTeleportEvents;
import io.ampznetwork.lunararc.forge.event.ForgeEntityJoinEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

@Mod("lunararc")
public final class LunarArcForge {

    public LunarArcForge() {
        // Same as NeoForge: FML shows a mod constructor's exception on its error screen.
        LunarArcClientSideGuard.requireDedicatedServer(FMLEnvironment.dist == Dist.CLIENT);
        LunarArcServer.installPlatform("Forge", LunarArcForge.class.getClassLoader());
        io.ampznetwork.lunararc.common.config.PluginBlacklist.screenLoadedMods(
                net.minecraftforge.fml.ModList.get().getMods().stream()
                        .collect(java.util.HashMap::new,
                                (map, mod) -> map.put(mod.getModId(), mod.getVersion().toString()),
                                java.util.HashMap::putAll));
        ForgeCommandHook.install();
        ForgeServerLifecycle.register(MinecraftForge.EVENT_BUS);
        ForgeChannelRegistration.register(MinecraftForge.EVENT_BUS);
        ForgeBlockBreakEvents.register(MinecraftForge.EVENT_BUS);
        ForgeBlockPlaceEvents.register(MinecraftForge.EVENT_BUS);
        ForgeEntityTeleportEvents.register(MinecraftForge.EVENT_BUS);
        ForgeEntityJoinEvents.register(MinecraftForge.EVENT_BUS);
    }
}
