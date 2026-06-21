package io.ampznetwork.lunararc.common.mixin.api;

import io.ampznetwork.lunararc.common.server.LunarArcServerBuildInfo;
import io.papermc.paper.ServerBuildInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = ServerBuildInfo.class, remap = false)
public interface ServerBuildInfoMixin {

    /**
     * @author LunarArc
     * @reason Return our Paper-compatible ServerBuildInfo so plugins that call
     *         ServerBuildInfo.buildInfo() don't hit Paper's ServerBuildInfoImpl
     *         which tries to read a version.json that doesn't exist on NeoForge.
     */
    @Overwrite
    static ServerBuildInfo buildInfo() {
        return LunarArcServerBuildInfo.INSTANCE;
    }
}
