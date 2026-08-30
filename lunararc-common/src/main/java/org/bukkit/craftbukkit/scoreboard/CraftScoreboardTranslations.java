package org.bukkit.craftbukkit.scoreboard;

import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.RenderType;

public final class CraftScoreboardTranslations {
    private CraftScoreboardTranslations() {}

    public static DisplaySlot toBukkitSlot(net.minecraft.world.scores.DisplaySlot minecraft) {
        return DisplaySlot.NAMES.value(minecraft.getSerializedName());
    }

    public static net.minecraft.world.scores.DisplaySlot fromBukkitSlot(DisplaySlot slot) {
        net.minecraft.world.scores.DisplaySlot result = net.minecraft.world.scores.DisplaySlot.CODEC.byName(slot.getId());
        if (result == null) throw new IllegalArgumentException("Unknown display slot " + slot);
        return result;
    }

    static RenderType toBukkitRender(ObjectiveCriteria.RenderType render) {
        return RenderType.valueOf(render.name());
    }

    static ObjectiveCriteria.RenderType fromBukkitRender(RenderType render) {
        return ObjectiveCriteria.RenderType.valueOf(render.name());
    }
}
