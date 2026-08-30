package io.ampznetwork.lunararc.common.bridge.access;

import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Normal runtime bridge implemented by the corresponding Mixin accessor/invoker. */
public interface MinecraftServerAccessBridge {
    long[] lunararc$getTickTimesNanos();
    int lunararc$getTickCount();
    Map<ResourceKey<Level>, ServerLevel> lunararc$getLevels();
}
