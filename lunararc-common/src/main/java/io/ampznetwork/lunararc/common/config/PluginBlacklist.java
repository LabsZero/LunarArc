package io.ampznetwork.lunararc.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Plugins and mods LunarArc refuses to run, or warns about, compiled into the build.
 *
 * <p>This used to be {@code lunararc-blacklist.yml}, written out on first start for the server
 * operator to edit. That was the wrong shape for what the list is. An entry here is not a
 * preference - it is LunarArc stating that a specific plugin or mod is known to break on it, which
 * is knowledge the project has and the operator does not. A file that ships empty and invites
 * editing puts the burden on the person least able to carry it, and an operator who deleted or
 * corrupted it silently lost every protection in it.</p>
 *
 * <h2>Adding an entry</h2>
 *
 * <p>Add a line to {@link #PLUGINS} or {@link #MODS} below and rebuild. Both take a name, an
 * optional exact version, and a reason. The reason is shown to the operator verbatim, so write it
 * as the explanation they will read when their server refuses to load something: name the symptom
 * and, where one exists, the version or alternative that works.</p>
 *
 * <p>Pass {@code null} for the version to cover every version. Pin a version only when a newer or
 * older one is genuinely fine - a pin that outlives its truth is worse than no entry, because it
 * reads as a considered judgement.</p>
 */
public final class PluginBlacklist {

    private static final Logger LOGGER = LoggerFactory.getLogger("LunarArc");

    /**
     * Bukkit/Spigot/Paper plugins refused at load time.
     *
     * <p>Matched on the name in plugin.yml or paper-plugin.yml, case-insensitively. A plugin
     * listed here is never constructed: the load fails with the reason below, in the same way an
     * unsatisfied dependency does.</p>
     */
    private static final List<Entry> PLUGINS = List.of(
            // new Entry("ExamplePlugin", "1.0.0", "Crashes on NeoForge; use 1.0.1 or newer.")
    );

    /**
     * Mod IDs known to be incompatible.
     *
     * <p>Matched on the loader's mod ID, case-insensitively. Unlike a plugin, a mod is already
     * loaded and running by the time LunarArc can see it - the loader resolved and constructed it
     * long before any of our code runs - so this cannot refuse one. It warns, loudly and by name,
     * which is the honest limit of what is possible here and is still worth having: it turns "the
     * server behaves strangely" into a line naming the cause.</p>
     */
    private static final List<Entry> MODS = List.of(
            // new Entry("examplemod", null, "Replaces the chunk system; conflicts with LunarArc's chunk handling.")
    );

    private PluginBlacklist() {
    }

    /**
     * The entry blocking a plugin, or {@code null} if it is allowed.
     *
     * @param pluginName    name from the plugin descriptor
     * @param pluginVersion version from the plugin descriptor
     */
    public static Entry check(String pluginName, String pluginVersion) {
        return find(PLUGINS, pluginName, pluginVersion);
    }

    /**
     * The entry naming a mod as incompatible, or {@code null} if it is not listed.
     *
     * @param modId   the loader's mod ID
     * @param version the mod's version, or {@code null} if the loader does not report one
     */
    public static Entry checkMod(String modId, String version) {
        return find(MODS, modId, version);
    }

    /**
     * Warn about any loaded mod that is listed, once, at startup.
     *
     * <p>Called by each loader module with the mods that loader resolved, since only the loader
     * can enumerate them.</p>
     *
     * @param loadedMods mod ID to version; a null version matches an entry with no version pinned
     */
    public static void screenLoadedMods(Map<String, String> loadedMods) {
        if (MODS.isEmpty() || loadedMods == null || loadedMods.isEmpty()) return;
        for (Map.Entry<String, String> mod : loadedMods.entrySet()) {
            Entry listed = checkMod(mod.getKey(), mod.getValue());
            if (listed == null) continue;
            LOGGER.warn("Mod '{}'{} is known to be incompatible with LunarArc: {}",
                    mod.getKey(),
                    mod.getValue() == null ? "" : " " + mod.getValue(),
                    listed.reason);
            LOGGER.warn("It is already loaded and cannot be stopped from here. Remove it from the "
                    + "mods folder if the server misbehaves.");
        }
    }

    private static Entry find(List<Entry> entries, String name, String version) {
        if (name == null) return null;
        for (Entry entry : entries) {
            if (!entry.name.equalsIgnoreCase(name)) continue;
            if (entry.version == null || entry.version.equals(version)) return entry;
        }
        return null;
    }

    /**
     * One blocked plugin or mod.
     *
     * @param name    plugin name or mod ID, matched case-insensitively
     * @param version exact version to match, or {@code null} for every version
     * @param reason  shown to the operator verbatim
     */
    public record Entry(String name, String version, String reason) {
    }
}
