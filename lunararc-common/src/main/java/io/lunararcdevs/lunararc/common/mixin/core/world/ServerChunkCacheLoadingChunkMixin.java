package io.lunararcdevs.lunararc.common.mixin.core.world;

import io.lunararcdevs.lunararc.common.event.LunarArcLoadingChunk;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerChunkCache.class)
public abstract class ServerChunkCacheLoadingChunkMixin {
    @Inject(method = "getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/ChunkAccess;",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void lunararc$loadingChunk(int x, int z, ChunkStatus status, boolean load, CallbackInfoReturnable<ChunkAccess> cir) {
        LevelChunk loading = LunarArcLoadingChunk.at(x, z);
        if (loading != null) cir.setReturnValue(loading);
    }
}
