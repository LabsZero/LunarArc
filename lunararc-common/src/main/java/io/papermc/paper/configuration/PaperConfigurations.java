package io.papermc.paper.configuration;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

/**
 * Real Paper's {@code PaperConfigurations} is its entire configuration system (global/world
 * config nodes, Configurate-based serialization, migration, the works) — a much larger
 * undertaking than anything ported here. This class intentionally contains only the one real,
 * self-contained static method {@link PluginInitializerManager} actually needs
 * ({@link #loadLegacyConfigFile}), ported verbatim from the real patch
 * (patches/server/0005-Paper-config-files.patch). It is not a stand-in for the rest of Paper's
 * config system — nothing else from that system should be assumed to exist here.
 */
public final class PaperConfigurations {

    private PaperConfigurations() {}

    @Deprecated
    public static YamlConfiguration loadLegacyConfigFile(File configFile) throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        if (configFile.exists()) {
            try {
                config.load(configFile);
            } catch (Exception ex) {
                throw new Exception("Failed to load configuration file: " + configFile.getName(), ex);
            }
        }
        return config;
    }
}
