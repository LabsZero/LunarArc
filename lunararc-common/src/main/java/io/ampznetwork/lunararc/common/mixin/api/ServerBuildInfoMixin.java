package io.ampznetwork.lunararc.common.mixin.api;

import io.ampznetwork.lunararc.common.server.LunarArcServerBuildInfo;
import io.papermc.paper.ServerBuildInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = ServerBuildInfo.class, remap = false)
public interface ServerBuildInfoMixin {


    @Overwrite
    static ServerBuildInfo buildInfo() {
        return LunarArcServerBuildInfo.INSTANCE;
    }
}
