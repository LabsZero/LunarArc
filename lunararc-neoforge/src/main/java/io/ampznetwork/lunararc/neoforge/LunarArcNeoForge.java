package io.ampznetwork.lunararc.neoforge;

import io.ampznetwork.lunararc.common.LunarArcClientSideGuard;
import io.ampznetwork.lunararc.common.mod.server.LunarArcServer;
import io.ampznetwork.lunararc.neoforge.command.NeoForgeCommandHook;
import io.ampznetwork.lunararc.neoforge.server.NeoForgeServerLifecycle;
import io.ampznetwork.lunararc.neoforge.event.NeoForgeBlockBreakEvents;
import io.ampznetwork.lunararc.neoforge.event.NeoForgeBlockPlaceEvents;
import io.ampznetwork.lunararc.neoforge.event.NeoForgeEntityTeleportEvents;
import io.ampznetwork.lunararc.neoforge.event.NeoForgeEntityJoinEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod("lunararc")
public final class LunarArcNeoForge {

    public LunarArcNeoForge(IEventBus modBus) {
        // Before anything is wired up: FML surfaces an exception thrown from a mod constructor
        // on its mod-loading error screen, so this is what a client user actually reads.
        LunarArcClientSideGuard.requireDedicatedServer(FMLEnvironment.dist == Dist.CLIENT);
        LunarArcServer.installPlatform("NeoForge", LunarArcNeoForge.class.getClassLoader());
        io.ampznetwork.lunararc.common.config.PluginBlacklist.screenLoadedMods(
                net.neoforged.fml.ModList.get().getMods().stream()
                        .collect(java.util.HashMap::new,
                                (map, mod) -> map.put(mod.getModId(), mod.getVersion().toString()),
                                java.util.HashMap::putAll));
        NeoForgeCommandHook.install();
        NeoForgeServerLifecycle.register();
        NeoForgeBlockBreakEvents.register();
        NeoForgeBlockPlaceEvents.register();
        NeoForgeEntityTeleportEvents.register();
        NeoForgeEntityJoinEvents.register();
    }
}
