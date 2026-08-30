package io.ampznetwork.lunararc.common.bridge.access;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.server.level.ChunkHolder;

/** Runtime-safe access to loader-owned ChunkMap state. */
public interface ChunkMapAccessBridge {
    Long2ObjectLinkedOpenHashMap<ChunkHolder> lunararc$getVisibleChunkMap();
}
