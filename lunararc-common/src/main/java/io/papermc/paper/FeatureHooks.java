package io.papermc.paper;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainerFactory;

/**
 * The slice of Paper's FeatureHooks that donated CraftBukkit classes call. Paper's full class wires Moonrise's chunk system,
 * which LunarArc does not ship, so only the vanilla-compatible hook lives here.
 */
public final class FeatureHooks {
    private FeatureHooks() {}

    public static LevelChunkSection createSection(final PalettedContainerFactory palettedContainerFactory, final Level level, final ChunkPos chunkPos, final int chunkSection) {
        return new LevelChunkSection(palettedContainerFactory);
    }

    public static io.papermc.paper.world.chunk.ChunkGenerationState getChunkGenerationState(final net.minecraft.server.level.ServerLevel level, final int x, final int z) {
        final net.minecraft.world.level.chunk.LevelChunk loaded = level.getChunkSource().getChunkNow(x, z);
        if (loaded != null) {
            return io.papermc.paper.world.chunk.ChunkGenerationState.GENERATED;
        }
        return getChunkGenerationStateAsync(level, x, z).join();
    }

    public static java.util.concurrent.CompletableFuture<io.papermc.paper.world.chunk.ChunkGenerationState> getChunkGenerationStateAsync(final net.minecraft.server.level.ServerLevel level, final int x, final int z) {
        final net.minecraft.world.level.chunk.LevelChunk loaded = level.getChunkSource().getChunkNow(x, z);
        if (loaded != null) {
            return java.util.concurrent.CompletableFuture.completedFuture(io.papermc.paper.world.chunk.ChunkGenerationState.GENERATED);
        }
        return level.getChunkSource().chunkMap.read(new net.minecraft.world.level.ChunkPos(x, z))
                .thenApply(tag -> classifyChunkGenerationState(tag.orElse(null)));
    }

    private static io.papermc.paper.world.chunk.ChunkGenerationState classifyChunkGenerationState(final net.minecraft.nbt.CompoundTag tag) {
        if (tag == null) {
            return io.papermc.paper.world.chunk.ChunkGenerationState.NOT_GENERATED;
        }
        final String status = tag.getStringOr("Status", "");
        if (status.isEmpty() || status.equals("minecraft:empty") || status.equals("empty")) {
            return io.papermc.paper.world.chunk.ChunkGenerationState.NOT_GENERATED;
        }
        if (!(status.equals("minecraft:full") || status.equals("full"))) {
            return io.papermc.paper.world.chunk.ChunkGenerationState.PARTIAL;
        }
        final int dataVersion = tag.getIntOr("DataVersion", 0);
        return dataVersion < net.minecraft.SharedConstants.getCurrentVersion().dataVersion().version()
                ? io.papermc.paper.world.chunk.ChunkGenerationState.OUTDATED
                : io.papermc.paper.world.chunk.ChunkGenerationState.GENERATED;
    }
}
