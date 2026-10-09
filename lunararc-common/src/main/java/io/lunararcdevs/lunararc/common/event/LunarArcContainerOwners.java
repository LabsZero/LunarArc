package io.lunararcdevs.lunararc.common.event;

import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public final class LunarArcContainerOwners {
    private static final Map<Container, Entity> OWNERS = Collections.synchronizedMap(new WeakHashMap<>());

    private LunarArcContainerOwners() {}

    public static void register(Container container, Entity owner) {
        if (container != null) OWNERS.put(container, owner);
    }

    public static Entity owner(Container container) {
        return OWNERS.get(container);
    }
}
