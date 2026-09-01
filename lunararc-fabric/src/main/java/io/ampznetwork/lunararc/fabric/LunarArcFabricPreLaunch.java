package io.ampznetwork.lunararc.fabric;

import io.ampznetwork.lunararc.common.LunarArcClientSideGuard;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

/**
 * Stops a client launch before Minecraft is loaded at all.
 *
 * <p>Fabric Loader runs pre-launch entrypoints before the game's own classes are touched, and
 * renders anything thrown out of one in its error window rather than only in a log. That is both
 * the earliest point at which LunarArc can tell a client user what is wrong and the point at which
 * the message is most likely to be read, so the check lives here rather than waiting for mod
 * initialization.</p>
 *
 * <p>Running this early also means no mixin from a server-only configuration has been applied to a
 * client class yet, so the failure is LunarArc's own explanation rather than whatever a
 * server-shaped mixin would have done to a client.</p>
 */
public final class LunarArcFabricPreLaunch implements PreLaunchEntrypoint {

    @Override
    public void onPreLaunch() {
        LunarArcClientSideGuard.requireDedicatedServer(
                "Fabric", FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT);
    }
}
