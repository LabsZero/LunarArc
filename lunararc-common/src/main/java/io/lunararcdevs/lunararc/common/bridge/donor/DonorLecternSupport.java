package io.lunararcdevs.lunararc.common.bridge.donor;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.LecternBlockEntity;

public final class DonorLecternSupport {
    private static final Map<Container, LecternBlockEntity> LECTERNS = Collections.synchronizedMap(new WeakHashMap<>());

    private DonorLecternSupport() {
    }

    public static void register(Container bookAccess, LecternBlockEntity lectern) {
        LECTERNS.put(bookAccess, lectern);
    }

    public static LecternBlockEntity lecternOf(Container bookAccess) {
        return LECTERNS.get(bookAccess);
    }
}
