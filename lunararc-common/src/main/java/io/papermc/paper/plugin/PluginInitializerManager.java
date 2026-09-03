package io.papermc.paper.plugin;

import io.papermc.paper.configuration.PaperConfigurations;
import io.papermc.paper.plugin.entrypoint.Entrypoint;
import io.papermc.paper.plugin.entrypoint.LaunchEntryPointHandler;
import io.papermc.paper.plugin.provider.PluginProvider;
import io.papermc.paper.plugin.provider.type.paper.PaperPluginParent;
import joptsimple.OptionSet;
import net.minecraft.server.dedicated.DedicatedServer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class PluginInitializerManager {

    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(PluginInitializerManager.class);
    private static PluginInitializerManager impl;
    private final Path pluginDirectory;
    private final Path updateDirectory;

    private static OptionSet lastOptionSet;

    PluginInitializerManager(final Path pluginDirectory, final Path updateDirectory) {
        this.pluginDirectory = pluginDirectory;
        this.updateDirectory = updateDirectory;
    }

    private static PluginInitializerManager parse(@NotNull final OptionSet minecraftOptionSet) throws Exception {

        final File configFileLocationBukkit = lunararc$readFileOption(minecraftOptionSet, "bukkit-settings", new File("bukkit.yml"));
        final Path pluginDirectory = lunararc$readFileOption(minecraftOptionSet, "plugins", new File("plugins")).toPath();

        final YamlConfiguration configuration = PaperConfigurations.loadLegacyConfigFile(configFileLocationBukkit);

        final String updateDirectoryName = configuration.getString("settings.update-folder", "update");
        if (updateDirectoryName.isBlank()) {
            return new PluginInitializerManager(pluginDirectory, null);
        }

        final Path resolvedUpdateDirectory = pluginDirectory.resolve(updateDirectoryName);
        if (!Files.isDirectory(resolvedUpdateDirectory)) {
            if (Files.exists(resolvedUpdateDirectory)) {
                LOGGER.error("Misconfigured update directory!");
                LOGGER.error("Your configured update directory ({}) in bukkit.yml is pointing to a non-directory path. " +
                    "Auto updating functionality will not work.", resolvedUpdateDirectory);
            }
            return new PluginInitializerManager(pluginDirectory, null);
        }

        boolean isSameFile;
        try {
            isSameFile = Files.isSameFile(resolvedUpdateDirectory, pluginDirectory);
        } catch (final IOException e) {
            LOGGER.error("Misconfigured update directory!");
            LOGGER.error("Failed to compare update/plugin directory", e);
            return new PluginInitializerManager(pluginDirectory, null);
        }

        if (isSameFile) {
            LOGGER.error("Misconfigured update directory!");
            LOGGER.error(("Your configured update directory (%s) in bukkit.yml is pointing to the same location as the plugin directory (%s). " +
                "Disabling auto updating functionality.").formatted(resolvedUpdateDirectory, pluginDirectory));

            return new PluginInitializerManager(pluginDirectory, null);
        }

        return new PluginInitializerManager(pluginDirectory, resolvedUpdateDirectory);
    }

    public static PluginInitializerManager init(final OptionSet optionSet) throws Exception {
        impl = parse(optionSet);
        return impl;
    }

    public static PluginInitializerManager instance() {
        if (impl == null) {
            impl = new PluginInitializerManager(new File("plugins").toPath(), null);
        }
        return impl;
    }

    @NotNull
    public Path pluginDirectoryPath() {
        return pluginDirectory;
    }

    @Nullable
    public Path pluginUpdatePath() {
        return updateDirectory;
    }

    public static void load(OptionSet optionSet) throws Exception {
        lastOptionSet = optionSet;

        io.papermc.paper.plugin.PluginInitializerManager pluginSystem = io.papermc.paper.plugin.PluginInitializerManager.init(optionSet);

        io.papermc.paper.plugin.util.EntrypointUtil.registerProvidersFromSource(io.papermc.paper.plugin.provider.source.DirectoryProviderSource.INSTANCE, pluginSystem.pluginDirectoryPath());

        @SuppressWarnings("unchecked")
        java.util.List<Path> files = lunararc$readFileListOption(optionSet, "add-plugin").stream().map(File::toPath).toList();
        io.papermc.paper.plugin.util.EntrypointUtil.registerProvidersFromSource(io.papermc.paper.plugin.provider.source.PluginFlagProviderSource.INSTANCE, files);
    }

    public static void reload(DedicatedServer dedicatedServer) {

        LaunchEntryPointHandler.INSTANCE.populateProviderStorage();
        try {
            load(lastOptionSet);
        } catch (Exception e) {
            throw new RuntimeException("Failed to reload!", e);
        }

        boolean hasPaperPlugin = false;
        for (PluginProvider<?> provider : LaunchEntryPointHandler.INSTANCE.getStorage().get(Entrypoint.PLUGIN).getRegisteredProviders()) {
            if (provider instanceof PaperPluginParent.PaperServerPluginProvider) {
                hasPaperPlugin = true;
                break;
            }
        }

        if (hasPaperPlugin) {
            LOGGER.warn("======== WARNING ========");
            LOGGER.warn("You are reloading while having Paper plugins installed on your server.");
            LOGGER.warn("Paper plugins do NOT support being reloaded. This will cause some unexpected issues.");
            LOGGER.warn("=========================");
        }
    }

    private static File lunararc$readFileOption(OptionSet optionSet, String name, File fallback) {
        try {
            Object value = optionSet.valueOf(name);
            return value instanceof File file ? file : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    @SuppressWarnings("unchecked")
    private static java.util.List<File> lunararc$readFileListOption(OptionSet optionSet, String name) {
        try {
            Object value = optionSet.valuesOf(name);
            return value instanceof java.util.List<?> list ? (java.util.List<File>) list : java.util.List.of();
        } catch (Exception e) {
            return java.util.List.of();
        }
    }
}