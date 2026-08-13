package org.bukkit.plugin.java;

import io.papermc.paper.plugin.configuration.PluginMeta;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.PluginBase;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Hybrid Bridge JavaPlugin implementation.
 */
public abstract class JavaPlugin extends PluginBase implements org.bukkit.command.TabExecutor {
    private boolean isEnabled = false;
    protected org.bukkit.plugin.PluginLoader loader;
    protected Server server;
    protected PluginDescriptionFile description;
    protected File dataFolder;
    protected File file;
    protected ClassLoader classLoader;
    protected PluginMeta pluginMeta;
    protected Logger logger;
    private boolean naggable = true;
    private FileConfiguration newConfig;
    private File configFile;
    private io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager<org.bukkit.plugin.Plugin> lifecycleManager;

    protected JavaPlugin() {
        final ClassLoader classLoader = this.getClass().getClassLoader();
        if (!(classLoader instanceof PluginClassLoader)) {
            throw new IllegalStateException("JavaPlugin requires " + PluginClassLoader.class.getName());
        }
        ((PluginClassLoader) classLoader).initialize(this);
    }

    @Override
    public final @NotNull File getDataFolder() {
        return dataFolder;
    }

    @Override
    public final @NotNull PluginDescriptionFile getDescription() {
        if (description == null) {
            throw new IllegalStateException("Plugin has not been initialized");
        }
        return description;
    }

    @Override
    public @NotNull FileConfiguration getConfig() {
        if (newConfig == null) {
            reloadConfig();
        }
        return newConfig;
    }

    @Override
    public void reloadConfig() {
        if (configFile == null) {
            throw new IllegalStateException("Plugin configuration is not available before plugin initialization");
        }
        newConfig = YamlConfiguration.loadConfiguration(configFile);
    }

    @Override
    public void saveConfig() {
        try {
            getConfig().save(configFile);
        } catch (Exception ex) {
            logger.severe("Could not save config to " + configFile);
        }
    }

    @Override
    public void saveDefaultConfig() {
        if (configFile == null) {
            throw new IllegalStateException("Plugin configuration is not available before plugin initialization");
        }
        if (!configFile.exists()) {
            saveResource("config.yml", false);
        }
    }

    @Override
    public void saveResource(@NotNull String resourcePath, boolean replace) {
        if (resourcePath == null || resourcePath.equals("")) {
            throw new IllegalArgumentException("ResourcePath cannot be null or empty");
        }

        resourcePath = resourcePath.replace('\\', '/');
        InputStream in = getResource(resourcePath);
        if (in == null) {
            throw new IllegalArgumentException("The embedded resource '" + resourcePath + "' cannot be found");
        }

        File outFile = new File(dataFolder, resourcePath);
        int lastIndex = resourcePath.lastIndexOf('/');
        File outDir = new File(dataFolder, resourcePath.substring(0, lastIndex >= 0 ? lastIndex : 0));

        if (!outDir.exists()) {
            outDir.mkdirs();
        }

        try {
            if (!outFile.exists() || replace) {
                OutputStream out = new FileOutputStream(outFile);
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.close();
                in.close();
            }
        } catch (Exception ex) {
            logger.severe("Could not save " + outFile.getName() + " to " + outFile);
        }
    }

    @Override
    public @Nullable InputStream getResource(@NotNull String filename) {
        return getClass().getClassLoader().getResourceAsStream(filename);
    }

    @Override
    public final @NotNull PluginLoader getPluginLoader() {
        return loader;
    }

    @Override
    public final @NotNull Server getServer() {
        return server;
    }

    @Override
    public final boolean isEnabled() {
        return isEnabled;
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onLoad() {
    }

    @Override
    public void onEnable() {
    }

    public void setEnabled(boolean enabled) {
        if (isEnabled != enabled) {
            isEnabled = enabled;
            if (isEnabled) {
                logger.info("Enabling " + getDescription().getFullName());
                onEnable();
            } else {
                logger.info("Disabling " + getDescription().getFullName());
                try {
                    onDisable();
                } catch (Throwable t) {
                    // Guard: onDisable() must never propagate — a crash here would
                    // mask the original onEnable() exception (e.g. handleCrash pattern).
                    logger.log(java.util.logging.Level.SEVERE,
                            "Error in onDisable() for " + getDescription().getFullName(), t);
                }
            }
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String alias, @NotNull String[] args) {
        return null;
    }

    /**
     * Internal Paper 1.21.1 initialization method.
     */
    public final void init(@NotNull Server server, @NotNull PluginDescriptionFile description, @NotNull File dataFolder,
            @NotNull File file, @NotNull ClassLoader classLoader, @NotNull PluginMeta pluginMeta,
            @NotNull Logger logger) {
        this.server = server;
        this.description = description;
        this.dataFolder = dataFolder;
        this.file = file;
        this.classLoader = classLoader;
        this.pluginMeta = pluginMeta;
        this.logger = logger;
        this.configFile = new File(dataFolder, "config.yml");

        this.lifecycleManager = io.ampznetwork.lunararc.common.server.LunarArcLifecycleEventManager.create();
        if (!(classLoader instanceof PluginClassLoader pluginClassLoader)) {
            throw new IllegalArgumentException("Plugin class loader is not a PluginClassLoader");
        }
        this.loader = pluginClassLoader.getPluginLoaderInstance();
    }

    @NotNull
    public static <T extends JavaPlugin> T getPlugin(@NotNull Class<T> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("Class cannot be null");
        }
        ClassLoader classLoader = clazz.getClassLoader();
        if (!(classLoader instanceof PluginClassLoader pluginClassLoader)) {
            throw new IllegalArgumentException(clazz + " is not initialized by " + PluginClassLoader.class.getName());
        }
        JavaPlugin plugin = pluginClassLoader.getPluginInstance();
        if (plugin == null) {
            throw new IllegalStateException("Cannot get plugin for " + clazz + " before it is initialized");
        }
        return clazz.cast(plugin);
    }

    @NotNull
    public static JavaPlugin getProvidingPlugin(@NotNull Class<?> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("Class cannot be null");
        }
        ClassLoader classLoader = clazz.getClassLoader();
        if (!(classLoader instanceof PluginClassLoader pluginClassLoader)) {
            throw new IllegalArgumentException(clazz + " is not provided by a plugin");
        }
        JavaPlugin plugin = pluginClassLoader.getPluginInstance();
        if (plugin == null) {
            throw new IllegalStateException("Cannot get providing plugin for " + clazz + " before it is initialized");
        }
        return plugin;
    }

    @Nullable
    public org.bukkit.command.PluginCommand getCommand(@NotNull String name) {
        String search = name.toLowerCase(java.util.Locale.ENGLISH);
        org.bukkit.command.Command command = server.getCommandMap().getCommand(search);
        if (command instanceof org.bukkit.command.PluginCommand pc && pc.getPlugin() == this) {
            return pc;
        }
        command = server.getCommandMap().getCommand(description.getName().toLowerCase(java.util.Locale.ENGLISH) + ":" + search);
        if (command instanceof org.bukkit.command.PluginCommand pc && pc.getPlugin() == this) {
            return pc;
        }
        // Fallback: try to find it in the description if it's not registered yet
        if (description.getCommands() != null && description.getCommands().containsKey(search)) {
            // Return a proxy if needed, but usually it should be in the command map
        }
        return null;
    }

    @Override
    public @NotNull PluginMeta getPluginMeta() {
        return pluginMeta;
    }

    public @Nullable String getAPIVersion() {
        return description.getAPIVersion();
    }

    public @NotNull List<String> getPluginLibraries() {
        return Collections.emptyList();
    }

    public @NotNull Logger getLogger() {
        if (logger == null) {
            return Logger.getLogger(description != null ? description.getName() : "UnknownPlugin");
        }
        return logger;
    }

    @Override
    public @Nullable ChunkGenerator getDefaultWorldGenerator(@NotNull String worldName, @Nullable String id) {
        return null;
    }

    @Override
    public @Nullable BiomeProvider getDefaultBiomeProvider(@NotNull String worldName, @Nullable String id) {
        return null;
    }

    public @NotNull org.slf4j.Logger getSLF4JLogger() {
        return org.slf4j.LoggerFactory.getLogger(description.getName());
    }

    public @NotNull net.kyori.adventure.text.logger.slf4j.ComponentLogger getComponentLogger() {
        return net.kyori.adventure.text.logger.slf4j.ComponentLogger.logger(description.getName());
    }

    public @NotNull io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager<org.bukkit.plugin.Plugin> getLifecycleManager() {
        return lifecycleManager;
    }

}
