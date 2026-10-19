package io.lunararcdevs.lunararc.quilt.mixin.core.server;

import io.lunararcdevs.lunararc.common.bridge.MinecraftServerBridge;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin_Quilt implements MinecraftServerBridge {
    @Override
    public void lunararc$loaderLevelLoad(ServerLevel level) {
        ServerLevelEvents.LOAD.invoker().onLevelLoad((MinecraftServer) (Object) this, level);
    }

    @Override
    public void lunararc$loaderLevelUnload(ServerLevel level) {
        ServerLevelEvents.UNLOAD.invoker().onLevelUnload((MinecraftServer) (Object) this, level);
    }

    @Override
    public boolean lunararc$loaderHandlesBlockBreakEvent() {
        return true;
    }
}
