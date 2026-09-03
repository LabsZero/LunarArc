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

/**
 * Gives Essentials' {@code /give}, {@code /item} and {@code /i} a name for every modded block and
 * item, the way <a href="https://github.com/atferrys/ModdedIntegration">atferrys/ModdedIntegration</a>
 * does for Mohist/Arclight - built in here, so no separate plugin is needed on LunarArc.
 *
 * <p>{@link LunarArcDynamicBukkitEnums} already gives every modded block and item a real Bukkit
 * {@link Material} constant, but that alone does not make it reachable by name through Essentials:
 * read directly from Essentials' own source (FlatItemDb.get(String), ItemData.getMaterial()),
 * name resolution goes only through {@code items.json}'s own closed vocabulary - {@code getByName}
 * checks its own {@code items}/{@code itemAliases} maps, built by {@code reloadConfig()} from that
 * file, and never falls back to {@code Material.matchMaterial}. {@code custom_items.yml},
 * Essentials' own sanctioned place for extra aliases, cannot help either:
 * {@code CustomItemResolver} only accepts a target that already resolves through
 * {@code getItemDb().get(...)}, i.e. an alias for an existing entry, not a new Material reference.
 * The entry has to go into {@code items.json} itself, which is exactly what ModdedIntegration does.
 *
 * <p>Once a top-level key exists there naming a Material, Essentials asks nothing more of it:
 * {@code ItemData.getMaterial()} decodes the {@code "material"} field through Gson's default enum
 * handling, which is {@code Enum.valueOf(Material.class, name)} - exactly the name
 * {@code LunarArcDynamicBukkitEnums} already gave the synthetic constant.
 */
public final class LunarArcEssentialsItemBridge {

    private static final Logger LOGGER = LoggerFactory.getLogger("LunarArc");
    private static final String ESSENTIALS_CLASS = "com.earth2me.essentials.Essentials";

    private LunarArcEssentialsItemBridge() {}

    /**
     * Called once, after every startup-phase plugin (Essentials included) has enabled - so its
     * own {@code reloadConfig()} has already extracted a default {@code items.json} if this is a
     * fresh install, and there is something on disk to merge into.
     */
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
                    + " - /give, /item and /i now accept them as <namespace>_<path>.");
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

    /**
     * Inserts one {@code "namespace_path": {"material": "ENUM_NAME"}} per modded item ahead of the
     * closing brace of the top-level object, leaving every other byte untouched.
     *
     * <p>A full JSON parse-and-reserialize round trip was deliberately avoided. Essentials'
     * {@code ManagedFile} appends a content hash and the extracting version as a footer after the
     * closing brace, used later to tell a stock file from a user-edited one; reformatting the
     * whole file would land on top of that footer and reorder or restyle entries nobody asked to
     * have touched. Editing only the lines between the header comments and the closing brace
     * leaves the footer, the header, and every existing entry exactly as they were.</p>
     */
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

        // The line before our insertion point is the last existing entry; JSON needs a comma
        // between it and what we are about to add, and items.json's own entries carry none on
        // their final line.
        int lastContentLine = closingBrace - 1;
        while (lastContentLine >= 0 && lines.get(lastContentLine).trim().isEmpty()) lastContentLine--;
        if (lastContentLine >= 0) {
            String content = lines.get(lastContentLine);
            String trimmed = content.trim();
            if (!trimmed.isEmpty() && !trimmed.endsWith(",") && !trimmed.endsWith("{")) {
                lines.set(lastContentLine, content + ",");
            }
        }

        // Our own last addition closes the object next, so it carries no trailing comma either.
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

    /**
     * Reflection only: {@code getItemDb()} and {@code reloadConfig()} are both public methods on
     * Essentials' own long-stable provider API ({@code IEssentials}/{@code IConf}), but this
     * module has no compile-time dependency on Essentials and should not gain one just to save a
     * server restart before {@code /give} sees the new entries.
     */
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
