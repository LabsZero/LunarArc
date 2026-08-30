package io.ampznetwork.lunararc.common.mixin.core.world;

import com.destroystokyo.paper.loottable.PaperLootableInventoryData;
import io.ampznetwork.lunararc.common.server.LunarArcLootableDataStorage;
import net.minecraft.world.RandomizableContainer;
import com.destroystokyo.paper.loottable.LootableInventory;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Real Paper adds {@code lootableData()} directly to this interface via source patch
 * (patches/server/0095-LootTable-API-and-replenishable-lootables.patch), backed by a genuine
 * field on every implementing class. LunarArc can't source-patch a vanilla interface, so this is
 * the same method backed by {@link LunarArcLootableDataStorage} instead — see that class's
 * javadoc for why that's the real equivalent technique, not a shortcut.
 * <p>
 * {@code getLootableInventory()} is NOT implemented for real here — real Paper's version
 * connects to the live Bukkit block-state wrapper for event-firing purposes, and LunarArc
 * doesn't have that live-wrapper infrastructure yet. Throws clearly rather than faking a wrong
 * implementation; this only affects Paper's lootable event-firing integration, not basic loot
 * generation, which containers already handle via vanilla RandomizableContainer mechanics.
 */
@Mixin(RandomizableContainer.class)
public interface RandomizableContainerMixin extends io.ampznetwork.lunararc.common.bridge.RandomizableContainerBridge {

    default PaperLootableInventoryData lootableData() {
        return LunarArcLootableDataStorage.get(this, PaperLootableInventoryData::new);
    }

    default LootableInventory getLootableInventory() {
        throw new UnsupportedOperationException(
                "Paper lootable event-firing integration is not implemented for RandomizableContainer yet");
    }
}
