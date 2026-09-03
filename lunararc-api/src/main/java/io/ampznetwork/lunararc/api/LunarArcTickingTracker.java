package io.ampznetwork.lunararc.api;

import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

public interface LunarArcTickingTracker {

    @Nullable
    Object getTickingSource();

    @Nullable
    Entity getTickingEntity();

    @Nullable
    Block getTickingBlock();

    @Nullable
    TileState getTickingBlockEntity();
}
