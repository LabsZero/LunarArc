package org.bukkit.craftbukkit.map;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.map.MapCursor;

public final class RenderData {
    public final byte[] buffer = new byte[128 * 128];
    public final List<MapCursor> cursors = new ArrayList<>();
}
