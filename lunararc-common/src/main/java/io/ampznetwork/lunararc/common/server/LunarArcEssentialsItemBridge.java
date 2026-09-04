package io.ampznetwork.lunararc.common.server;

import net.minecraft.resources.ResourceLocation;
import org.bukkit.Material;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Exposes loader-owned items to EssentialsX as both namespace_path and namespace:path. */
public final class LunarArcEssentialsItemBridge {

    private static final Logger LOGGER = LoggerFactory.getLogger("LunarArc");
    private static final String ESSENTIALS_CLASS = "com.earth2me.essentials.Essentials";

    private LunarArcEssentialsItemBridge() {}

    public static void populateModdedItems(CraftServer craftServer) {
        Plugin essentials = findEssentials(craftServer);
        if (essentials == null) return;

        try {
            File itemsFile = new File(essentials.getDataFolder(), "items.json");
            if (!itemsFile.isFile()) return;

            List<String> lines = Files.readAllLines(itemsFile.toPath(), StandardCharsets.UTF_8);
            List<String> updated = mergeModdedItems(lines);
            if (updated == null) return;

            Files.write(itemsFile.toPath(), updated, StandardCharsets.UTF_8);
            LOGGER.info("[LunarArc] Added modded items to Essentials' items.json"
                    + " - /give, /item and /i accept <namespace>_<path> and <namespace>:<path>.");
            reloadEssentialsItemDb(essentials);
        } catch (Exception e) {
            LOGGER.warn("[LunarArc] Could not add modded items to Essentials' items.json: {}", e.toString());
        }
    }

    private static Plugin findEssentials(CraftServer craftServer) {
        for (Plugin plugin : craftServer.getPluginManager().getPlugins()) {
            if (plugin.isEnabled() && ESSENTIALS_CLASS.equals(plugin.getClass().getName())) {
                return plugin;
            }
        }
        return null;
    }

    private static List<String> mergeModdedItems(List<String> lines) {
        int closingBrace = -1;
        for (int i = lines.size() - 1; i >= 0; i--) {
            String trimmed = lines.get(i).trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
            if (trimmed.equals("}")) closingBrace = i;
            break;
        }
        if (closingBrace < 0) return null;

        List<String> additions = new ArrayList<>();
        for (Map.Entry<ResourceLocation, Material> entry : LunarArcDynamicBukkitEnums.materialsById().entrySet()) {
            ResourceLocation id = entry.getKey();
            if ("minecraft".equals(id.getNamespace())) continue;
            Material material = entry.getValue();
            if (material == null || !material.isItem()) continue;

            String alias = (id.getNamespace() + "_" + id.getPath()).toLowerCase(Locale.ROOT);
            if (alreadyPresent(lines, alias)) continue;

            additions.add("  \"" + alias + "\": {");
            additions.add("    \"material\": \"" + material.name() + "\"");
            additions.add("  },");
        }
        if (additions.isEmpty()) return null;

        int lastContentLine = closingBrace - 1;
        while (lastContentLine >= 0 && lines.get(lastContentLine).trim().isEmpty()) lastContentLine--;
        if (lastContentLine >= 0) {
            String content = lines.get(lastContentLine);
            String trimmed = content.trim();
            if (!trimmed.isEmpty() && !trimmed.endsWith(",") && !trimmed.endsWith("{")) {
                lines.set(lastContentLine, content + ",");
            }
        }

        int lastAddition = additions.size() - 1;
        String lastLine = additions.get(lastAddition);
        additions.set(lastAddition, lastLine.substring(0, lastLine.length() - 1));

        List<String> result = new ArrayList<>(lines.size() + additions.size());
        result.addAll(lines.subList(0, closingBrace));
        result.addAll(additions);
        result.addAll(lines.subList(closingBrace, lines.size()));
        return result;
    }

    private static boolean alreadyPresent(List<String> lines, String alias) {
        String quoted = "\"" + alias + "\"";
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith(quoted + ":") || trimmed.startsWith(quoted + " :")) return true;
        }
        return false;
    }

    private static void reloadEssentialsItemDb(Plugin essentials) {
        try {
            Object itemDb = essentials.getClass().getMethod("getItemDb").invoke(essentials);
            itemDb.getClass().getMethod("reloadConfig").invoke(itemDb);
        } catch (ReflectiveOperationException e) {
            LOGGER.debug("[LunarArc] Could not refresh Essentials' item database in place;"
                    + " a restart will pick the new items up: {}", e.toString());
        }
    }
}
