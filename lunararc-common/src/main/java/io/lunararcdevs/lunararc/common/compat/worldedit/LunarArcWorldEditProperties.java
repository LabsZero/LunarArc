package io.lunararcdevs.lunararc.common.compat.worldedit;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.block.state.properties.Property;

public final class LunarArcWorldEditProperties {
    private LunarArcWorldEditProperties() {
    }

    public static List<String> valueNames(Object property) {
        return names((Property<?>) property);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static List<String> names(Property property) {
        List<String> names = new ArrayList<>();
        for (Object value : property.getPossibleValues()) {
            names.add(property.getName((Comparable) value));
        }
        return names;
    }
}
