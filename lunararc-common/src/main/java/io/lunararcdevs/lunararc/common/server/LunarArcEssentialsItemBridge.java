package io.lunararcdevs.lunararc.common.server;

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
    private static final AliasIndex<Material> MODDED_ITEM_ALIASES = new AliasIndex<>(
            LunarArcDynamicBukkitEnums::materialsById, Material::isItem);

    private LunarArcEssentialsItemBridge() {}

    private static volatile Plugin essentialsPlugin;
    private static volatile String[] essentialsNames = new String[0];
    private static volatile long essentialsNamesBuiltAt;
    private static final long REINDEX_NANOS = java.util.concurrent.TimeUnit.MINUTES.toNanos(2);
    private static final java.util.concurrent.atomic.AtomicBoolean INDEXING = new java.util.concurrent.atomic.AtomicBoolean();

    public static void populateModdedItems(CraftServer craftServer) {
        MODDED_ITEM_ALIASES.refresh();
        Plugin essentials = findEssentials(craftServer);
        if (essentials == null) return;
        essentialsPlugin = essentials;
        indexEssentialsNamesAsync();

        try {
            stripGeneratedEntries(essentials);
        } catch (Exception e) {
            LOGGER.warn("[LunarArc] Could not clean up Essentials' items.json: {}", e.toString());
        } finally {
            prepareItemCommands(essentials);
        }
    }

    private static void stripGeneratedEntries(Plugin essentials) throws Exception {
        File itemsFile = new File(essentials.getDataFolder(), "items.json");
        if (!itemsFile.isFile()) return;

        File marker = new File(essentials.getDataFolder(), ".lunararc-items-checked");
        if (stamp(itemsFile).equals(readMarker(marker))) return;

        List<String> lines = Files.readAllLines(itemsFile.toPath(), StandardCharsets.UTF_8);
        List<String> cleaned = withoutGeneratedEntries(lines);
        if (cleaned == null) {
            writeMarker(marker, stamp(itemsFile));
            return;
        }

        Files.write(itemsFile.toPath(), cleaned, StandardCharsets.UTF_8);
        LOGGER.info("[LunarArc] Removed {} modded item entries LunarArc used to write into Essentials' items.json;"
                + " they are now resolved on demand.", (lines.size() - cleaned.size()) / 3);
        reloadEssentialsItemDb(essentials);
        writeMarker(marker, stamp(itemsFile));
    }

    private static String stamp(File file) {
        return file.length() + ":" + file.lastModified();
    }

    private static String readMarker(File marker) {
        try {
            return marker.isFile() ? Files.readString(marker.toPath(), StandardCharsets.UTF_8).trim() : "";
        } catch (java.io.IOException error) {
            return "";
        }
    }

    private static void writeMarker(File marker, String value) {
        try {
            Files.writeString(marker.toPath(), value, StandardCharsets.UTF_8);
        } catch (java.io.IOException ignored) {
        }
    }

    private static Object getItemDb(Plugin essentials) throws ReflectiveOperationException {
        return essentials.getClass().getMethod("getItemDb").invoke(essentials);
    }

    private static void prepareItemCommands(Plugin essentials) {
        if (!essentials.isEnabled()) return;
        try {
            Object itemDb = getItemDb(essentials);
            itemDb.getClass().getMethod("get", String.class, boolean.class).invoke(itemDb, "stone", false);
            ClassLoader loader = essentials.getClass().getClassLoader();
            Class.forName("com.earth2me.essentials.commands.Commandgive", false, loader);
            Class.forName("com.earth2me.essentials.commands.Commanditem", false, loader);
        } catch (ReflectiveOperationException error) {
        }
    }

    private static volatile java.lang.reflect.Constructor<?> itemDataConstructor;

    public static Object moddedItemData(Object itemDb, String name) {
        Material material = resolveAlias(name);
        if (material == null) return null;
        try {
            java.lang.reflect.Constructor<?> constructor = itemDataConstructor;
            if (constructor == null) {
                Class<?> data = Class.forName("com.earth2me.essentials.items.FlatItemDb$ItemData", false,
                        itemDb.getClass().getClassLoader());
                constructor = data.getDeclaredConstructor(Material.class);
                constructor.setAccessible(true);
                itemDataConstructor = constructor;
            }
            return constructor.newInstance(material);
        } catch (ReflectiveOperationException error) {
            return null;
        }
    }

    private static void indexEssentialsNamesAsync() {
        if (!INDEXING.compareAndSet(false, true)) return;
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                Plugin essentials = essentialsPlugin;
                if (essentials == null) return;
                Object itemDb = getItemDb(essentials);
                java.util.Collection<?> names = (java.util.Collection<?>) itemDb.getClass().getMethod("listNames").invoke(itemDb);
                java.util.TreeSet<String> sorted = new java.util.TreeSet<>();
                for (Object name : new ArrayList<>(names)) {
                    if (name != null) sorted.add(name.toString().toLowerCase(Locale.ROOT));
                }
                essentialsNames = sorted.toArray(new String[0]);
                essentialsNamesBuiltAt = System.nanoTime();
            } catch (Throwable error) {
                LOGGER.debug("[LunarArc] Could not index Essentials item names: {}", error.toString());
            } finally {
                INDEXING.set(false);
            }
        });
    }

    /** Fast item-name completion for Essentials give/item, or null when this command or argument is not one. */
    public static List<String> itemSuggestions(org.bukkit.command.Command command, String[] args, int limit) {
        if (args.length == 0 || !(command instanceof org.bukkit.command.PluginIdentifiableCommand owned)
                || !ESSENTIALS_CLASS.equals(owned.getPlugin().getClass().getName())) {
            return null;
        }
        String name = command.getName();
        int itemArgument = name.equals("give") ? 1 : name.equals("item") ? 0 : -1;
        if (args.length - 1 != itemArgument) return null;
        String[] names = essentialsNames;
        if (names.length == 0) {
            indexEssentialsNamesAsync();
            return null;
        }
        if (System.nanoTime() - essentialsNamesBuiltAt > REINDEX_NANOS) indexEssentialsNamesAsync();

        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        MODDED_ITEM_ALIASES.refresh();
        java.util.TreeSet<String> merged = new java.util.TreeSet<>();
        collectPrefix(names, prefix, limit, merged);
        for (String alias : MODDED_ITEM_ALIASES.withPrefix(prefix, limit)) merged.add(alias);
        List<String> result = new ArrayList<>(Math.min(limit, merged.size()));
        for (String match : merged) {
            if (result.size() >= limit) break;
            result.add(match);
        }
        return result;
    }

    private static void collectPrefix(String[] sorted, String prefix, int limit, java.util.Collection<String> out) {
        int index = java.util.Arrays.binarySearch(sorted, prefix);
        if (index < 0) index = -index - 1;
        for (int taken = 0; index < sorted.length && taken < limit && sorted[index].startsWith(prefix); index++, taken++) {
            out.add(sorted[index]);
        }
    }

    private static final int MODDED_COMPLETION_LIMIT = 200;

    public static List<String> withModdedItems(org.bukkit.command.Command command, String[] args, List<String> completions) {
        if (args.length == 0 || !(command instanceof org.bukkit.command.PluginIdentifiableCommand owned)
                || !ESSENTIALS_CLASS.equals(owned.getPlugin().getClass().getName())) {
            return completions;
        }
        String name = command.getName();
        int itemArgument = name.equals("give") ? 1 : name.equals("item") ? 0 : -1;
        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        if (args.length - 1 != itemArgument || prefix.length() < 2) return completions;

        MODDED_ITEM_ALIASES.refresh();
        List<String> matches = MODDED_ITEM_ALIASES.withPrefix(prefix, MODDED_COMPLETION_LIMIT);
        if (matches.isEmpty()) return completions;
        List<String> merged = new ArrayList<>(completions == null ? matches.size() : completions.size() + matches.size());
        if (completions != null) merged.addAll(completions);
        merged.addAll(matches);
        return merged;
    }

    /** Resolves Essentials' namespace_path form without scanning every registered item. */
    public static Material resolveAlias(String alias) {
        if (alias == null || alias.isBlank()) return null;
        String normalized = alias.trim().toLowerCase(Locale.ROOT);
        return MODDED_ITEM_ALIASES.get(normalized);
    }

    static final class AliasIndex<T> {
        private final java.util.function.Supplier<Map<ResourceLocation, T>> source;
        private final java.util.function.Predicate<T> isItem;
        private volatile Snapshot<T> snapshot = new Snapshot<>(-1, Map.of(), new String[0]);

        AliasIndex(java.util.function.Supplier<Map<ResourceLocation, T>> source, java.util.function.Predicate<T> isItem) {
            this.source = source;
            this.isItem = isItem;
        }

        T get(String alias) {
            refresh();
            return snapshot.aliases().get(alias);
        }

        List<String> withPrefix(String prefix, int limit) {
            String[] sorted = snapshot.sorted();
            int index = java.util.Arrays.binarySearch(sorted, prefix);
            if (index < 0) index = -index - 1;
            List<String> matches = new ArrayList<>(Math.min(limit, 16));
            for (; index < sorted.length && matches.size() < limit && sorted[index].startsWith(prefix); index++) {
                matches.add(sorted[index]);
            }
            return matches;
        }

        void refresh() {
            Map<ResourceLocation, T> materials = source.get();
            if (snapshot.materialCount() == materials.size()) return;
            synchronized (this) {
                int count = materials.size();
                if (snapshot.materialCount() == count) return;
                Map<String, T> aliases = new java.util.HashMap<>();
                materials.forEach((id, material) -> {
                    if (id != null && material != null && !"minecraft".equals(id.getNamespace()) && isItem.test(material)) {
                        aliases.putIfAbsent(id.getNamespace() + "_" + id.getPath(), material);
                    }
                });
                String[] sorted = aliases.keySet().toArray(new String[0]);
                java.util.Arrays.sort(sorted);
                snapshot = new Snapshot<>(count, Map.copyOf(aliases), sorted);
            }
        }

        private record Snapshot<T>(int materialCount, Map<String, T> aliases, String[] sorted) {}
    }

    private static Plugin findEssentials(CraftServer craftServer) {
        for (Plugin plugin : craftServer.getPluginManager().getPlugins()) {
            if (plugin.isEnabled() && ESSENTIALS_CLASS.equals(plugin.getClass().getName())) {
                return plugin;
            }
        }
        return null;
    }

    private static List<String> withoutGeneratedEntries(List<String> lines) {
        Map<String, String> generated = new java.util.HashMap<>();
        for (Map.Entry<ResourceLocation, Material> entry : LunarArcDynamicBukkitEnums.materialsById().entrySet()) {
            ResourceLocation id = entry.getKey();
            Material material = entry.getValue();
            if (!"minecraft".equals(id.getNamespace()) && material != null && material.isItem()) {
                generated.put((id.getNamespace() + "_" + id.getPath()).toLowerCase(Locale.ROOT), material.name());
            }
        }

        List<String> kept = new ArrayList<>(lines.size());
        boolean removed = false;
        for (int i = 0; i < lines.size(); i++) {
            String alias = quotedKey(lines.get(i));
            String material = alias == null ? null : generated.get(alias);
            if (material != null && i + 2 < lines.size()
                    && lines.get(i + 1).trim().equals("\"material\": \"" + material + "\"")
                    && lines.get(i + 2).trim().startsWith("}")) {
                i += 2;
                removed = true;
                continue;
            }
            kept.add(lines.get(i));
        }
        if (!removed) return null;

        for (int i = kept.size() - 1; i > 0; i--) {
            String trimmed = kept.get(i).trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
            if (trimmed.equals("}")) {
                for (int j = i - 1; j >= 0; j--) {
                    String previous = kept.get(j);
                    if (previous.trim().isEmpty()) continue;
                    if (previous.trim().endsWith(",")) kept.set(j, previous.substring(0, previous.lastIndexOf(',')));
                    break;
                }
            }
            break;
        }
        return kept;
    }

    private static String quotedKey(String line) {
        String trimmed = line.trim();
        if (trimmed.length() < 4 || trimmed.charAt(0) != '"') return null;
        int endQuote = trimmed.indexOf('"', 1);
        if (endQuote <= 1) return null;
        int colon = endQuote + 1;
        while (colon < trimmed.length() && Character.isWhitespace(trimmed.charAt(colon))) colon++;
        return colon < trimmed.length() && trimmed.charAt(colon) == ':' && trimmed.endsWith("{")
                ? trimmed.substring(1, endQuote) : null;
    }

    private static void reloadEssentialsItemDb(Plugin essentials) {
        try {
            Object itemDb = getItemDb(essentials);
            itemDb.getClass().getMethod("reloadConfig").invoke(itemDb);
        } catch (ReflectiveOperationException e) {
            LOGGER.debug("[LunarArc] Could not refresh Essentials' item database in place;"
                    + " a restart will pick the new items up: {}", e.toString());
        }
    }
}
