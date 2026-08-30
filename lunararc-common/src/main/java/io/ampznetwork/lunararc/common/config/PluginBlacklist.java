package io.ampznetwork.lunararc.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class PluginBlacklist {

    private static final Logger LOGGER = LoggerFactory.getLogger("LunarArc");
    private static final String BLACKLIST_FILE = "lunararc-blacklist.yml";

    private static final List<BlacklistEntry> entries = new ArrayList<>();

    public static void load() {
        entries.clear();
        File file = new File(BLACKLIST_FILE);
        if (!file.exists()) {
            writeDefaults(file);
            return;
        }

        try (FileInputStream in = new FileInputStream(file)) {
            Yaml yaml = new Yaml();
            Map<String, Object> root = yaml.load(in);
            if (root == null) return;

            Object blacklist = root.get("blacklist");
            if (blacklist instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> map) {
                        String name = map.containsKey("name") ? String.valueOf(map.get("name")) : "";
                        String version = map.containsKey("version") ? String.valueOf(map.get("version")) : null;
                        String reason = map.containsKey("reason") ? String.valueOf(map.get("reason")) : "Incompatible with LunarArc";
                        if (!name.isEmpty()) {
                            entries.add(new BlacklistEntry(name, version, reason));
                        }
                    }
                }
            }

            LOGGER.debug("[LunarArc] Plugin blacklist loaded with {} entries.", entries.size());
        } catch (Exception e) {
            LOGGER.error("[LunarArc] Failed to load lunararc-blacklist.yml", e);
        }
    }

    public static BlacklistEntry check(String pluginName, String pluginVersion) {
        for (BlacklistEntry entry : entries) {
            if (!entry.name.equalsIgnoreCase(pluginName)) continue;
            if (entry.version == null || entry.version.equals(pluginVersion)) {
                return entry;
            }
        }
        return null;
    }

    private static void writeDefaults(File file) {
        String defaults = """
                # LunarArc plugin blacklist
                # Plugins listed here will be refused at load time.
                # 'version' is optional — omit to block all versions.
                blacklist:
                  # - name: ExamplePlugin
                  #   version: 1.0.0
                  #   reason: Crashes on NeoForge due to missing CraftChunk impl
                """;
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(defaults);
        } catch (IOException e) {
            LOGGER.error("[LunarArc] Could not write default lunararc-blacklist.yml", e);
        }
    }

    public static class BlacklistEntry {
        public final String name;
        public final String version;
        public final String reason;

        public BlacklistEntry(String name, String version, String reason) {
            this.name = name;
            this.version = version;
            this.reason = reason;
        }
    }
}
