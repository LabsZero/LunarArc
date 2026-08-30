package org.spigotmc;

import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Spigot configuration compatibility surface.
 *
 * <p>This class intentionally exposes the long-standing public/static fields
 * used by CraftBukkit/Paper integrations while leaving loader ownership and
 * server behaviour in LunarArc common code.</p>
 */
public final class SpigotConfig {
    public static final YamlConfiguration config = new YamlConfiguration();
    public static int version = 12;

    public static boolean bungee = false;
    public static boolean lateBind = false;
    public static boolean restartOnCrash = false;
    public static String restartScript = "./start.sh";
    public static int timeoutTime = 60;
    public static int nettyThreads = 4;
    public static int userCacheCap = 1000;
    public static double movedWronglyThreshold = 0.0625D;
    public static double movedTooQuicklyMultiplier = 10.0D;

    private SpigotConfig() {
    }

    /**
     * Populate the compatibility view from the server's spigot configuration.
     * Unknown options retain safe Spigot-compatible defaults.
     */
    public static void init(java.io.File file) {
        if (file == null) {
            return;
        }
        try {
            syncFrom(YamlConfiguration.loadConfiguration(file));
        } catch (RuntimeException ex) {
            java.util.logging.Logger.getLogger("SpigotConfig")
                    .log(java.util.logging.Level.WARNING, "Could not load spigot.yml compatibility view", ex);
        }
    }

    public static void syncFrom(YamlConfiguration loaded) {
        if (loaded == null) {
            return;
        }
        for (String key : loaded.getKeys(true)) {
            if (!loaded.isConfigurationSection(key)) {
                config.set(key, loaded.get(key));
            }
        }
        bungee = config.getBoolean("settings.bungeecord", bungee);
            lateBind = config.getBoolean("settings.late-bind", lateBind);
            restartOnCrash = config.getBoolean("settings.restart-on-crash", restartOnCrash);
            restartScript = config.getString("settings.restart-script", restartScript);
            timeoutTime = config.getInt("settings.timeout-time", timeoutTime);
            nettyThreads = config.getInt("settings.netty-threads", nettyThreads);
            userCacheCap = config.getInt("settings.user-cache-size", userCacheCap);
            movedWronglyThreshold = config.getDouble("settings.moved-wrongly-threshold", movedWronglyThreshold);
        movedTooQuicklyMultiplier = config.getDouble("settings.moved-too-quickly-multiplier", movedTooQuicklyMultiplier);
    }
}
