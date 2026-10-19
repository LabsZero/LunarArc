package io.papermc.paper.world.saveddata;

import org.bukkit.craftbukkit.persistence.CraftPersistentDataContainer;

/**
 * Minimal LunarArc version of Paper's per-world persistent data holder. Paper stores it as vanilla saved data, which relies on
 * a Paper-only {@code DataFixTypes} entry; LunarArc only needs the wrapper so plugin-facing code (WorldEdit's {@code //regen})
 * can pass a world's container along.
 */
public final class PaperWorldPDC {
    private final CraftPersistentDataContainer container;

    public PaperWorldPDC(final CraftPersistentDataContainer container) {
        this.container = container;
    }

    public CraftPersistentDataContainer getContainer() {
        return this.container;
    }
}
