package io.lunararcdevs.lunararc.common.mixin.core.events;

import io.lunararcdevs.lunararc.common.event.LunarArcGameplayEvents;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fires PlayerNaturallySpawnCreaturesEvent once per player per chunk-tick, before spawning looks at who is close enough. */
@Mixin(ServerChunkCache.class)
public abstract class GameplaySpawnTickMixin {
    @Shadow @org.spongepowered.asm.mixin.Final net.minecraft.server.level.ServerLevel level;

    @Inject(method = "tickChunks(Lnet/minecraft/util/profiling/ProfilerFiller;)V", at = @At("HEAD"), require = 1)
    private void lunararc$naturalSpawnEvents(ProfilerFiller profiler, CallbackInfo ci) {
        LunarArcGameplayEvents.fireNaturalSpawn(this.level);
    }
}
