package io.lunararcdevs.lunararc.common.server;

import io.lunararcdevs.lunararc.api.EnumHelper;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.craftbukkit.util.CraftLegacy;

public final class DynamicMaterialOrdinalRegressionTest {
    public static void run() {
        int start = Material.values().length;
        List<Material> added = List.of(
                EnumHelper.makeEnum(Material.class, "TEST_MOD_ONE", start, List.of(int.class), List.of(-1)),
                EnumHelper.makeEnum(Material.class, "TEST_MOD_TWO", start + 1, List.of(int.class), List.of(-1)));
        EnumHelper.addEnums(Material.class, added);

        Material[] modern = CraftLegacy.modern_values();
        for (int i = 0; i < modern.length; i++) {
            if (modern[i].isLegacy()) throw new AssertionError("legacy material in modern values: " + modern[i]);
            if (CraftLegacy.modern_ordinal(modern[i]) != i) {
                throw new AssertionError("modern ordinal mismatch for " + modern[i] + ": " + CraftLegacy.modern_ordinal(modern[i]) + " != " + i);
            }
        }
        if (modern[modern.length - 1] != added.get(1)) throw new AssertionError("dynamic material missing from modern values");
        System.out.println("Dynamic material ordinal regressions passed");
    }
}
