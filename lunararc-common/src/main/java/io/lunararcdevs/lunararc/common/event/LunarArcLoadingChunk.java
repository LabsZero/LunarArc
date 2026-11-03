package io.lunararcdevs.lunararc.common.event;

import net.minecraft.world.level.chunk.LevelChunk;

public final class LunarArcLoadingChunk {
    private static final ThreadLocal<LevelChunk> CURRENT = new ThreadLocal<>();

    private LunarArcLoadingChunk() {}

    public static LevelChunk enter(LevelChunk chunk) {
        LevelChunk previous = CURRENT.get();
        CURRENT.set(chunk);
        return previous;
    }

    public static void leave(LevelChunk previous) {
        if (previous == null) CURRENT.remove();
        else CURRENT.set(previous);
    }

    public static LevelChunk at(int x, int z) {
        LevelChunk chunk = CURRENT.get();
        return chunk != null && chunk.getPos().x == x && chunk.getPos().z == z ? chunk : null;
    }
}
