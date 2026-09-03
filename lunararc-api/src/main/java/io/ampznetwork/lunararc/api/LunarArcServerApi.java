package io.ampznetwork.lunararc.api;

import org.bukkit.plugin.Plugin;

public interface LunarArcServerApi {

    default LunarArcVersion getVersion() {
        return LunarArcVersion.current();
    }

    default LunarArcPlatform getPlatform() {
        return LunarArcPlatform.UNKNOWN;
    }

    void registerModEvent(Plugin plugin, Object eventBus, Object target);

    LunarArcTickingTracker getTickingTracker();
}
