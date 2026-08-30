package io.ampznetwork.lunararc.common.server;

import com.destroystokyo.paper.loottable.PaperLootableInventoryData;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;

/**
 * Real Paper attaches a {@code PaperLootableInventoryData} field directly to every
 * {@code RandomizableContainer}/{@code ContainerEntity} implementor via source patch. LunarArc
 * can't source-patch vanilla interfaces, so this is the equivalent real technique for attaching
 * per-instance state to a type you don't own: a weak, identity-keyed association map, backing
 * the {@code lootableData()} default methods Mixin adds to those two interfaces
 * (RandomizableContainerMixin / ContainerEntityMixin).
 */
public final class LunarArcLootableDataStorage {

    private static final Map<Object, PaperLootableInventoryData> DATA =
            Collections.synchronizedMap(new WeakHashMap<>());

    private LunarArcLootableDataStorage() {}

    public static PaperLootableInventoryData get(Object holder, Supplier<PaperLootableInventoryData> factory) {
        return DATA.computeIfAbsent(holder, ignored -> factory.get());
    }
}
