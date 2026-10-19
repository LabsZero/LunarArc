package io.papermc.paper.world;

import io.papermc.paper.world.saveddata.PaperLevelOverrides;
import io.papermc.paper.world.saveddata.PaperWorldPDC;
import java.util.UUID;
import net.minecraft.world.level.storage.ServerLevelData;
import org.jspecify.annotations.Nullable;

/**
 * Minimal LunarArc version of Paper's world loader: only the {@link LoadedWorldData} record that plugin-facing code
 * (WorldEdit's {@code //regen} adapters) constructs. LunarArc loads worlds itself, so the rest of Paper's loader,
 * with its folder-migration machinery, is deliberately not carried over.
 */
public final class PaperWorldLoader {
    private PaperWorldLoader() {
    }

    public record LoadedWorldData(
        String bukkitName,
        UUID uuid,
        @Nullable PaperWorldPDC pdc,
        @Nullable PaperLevelOverrides levelOverrides
    ) {
        /** Shape some WorldEdit adapters use: they hand over the live level data instead of Paper's overrides. */
        public LoadedWorldData(final String bukkitName, final UUID uuid, final @Nullable PaperWorldPDC pdc, final ServerLevelData levelData) {
            this(bukkitName, uuid, pdc, (PaperLevelOverrides) null);
        }
    }
}
