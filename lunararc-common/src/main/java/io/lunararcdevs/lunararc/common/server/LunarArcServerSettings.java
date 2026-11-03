package io.lunararcdevs.lunararc.common.server;

import io.lunararcdevs.lunararc.common.LunarArcServerAccess;
import io.lunararcdevs.lunararc.common.bridge.LevelBridge;
import io.papermc.paper.configuration.WorldConfiguration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.util.CraftSpawnCategory;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class LunarArcServerSettings {
    private LunarArcServerSettings() {}

    private static final WorldConfiguration.Tuning NONE = new WorldConfiguration.Tuning();


    public static WorldConfiguration.Tuning tuning(Level level) {
        try {
            WorldConfiguration configuration = ((LevelBridge) level).lunararc$getPaperConfiguration();
            return configuration == null ? NONE : configuration.tuning;
        } catch (Throwable notReady) {
            return NONE;
        }
    }

    public static boolean tickAllowed(Level level, BlockPos pos, int rate) {
        if (rate == 1) return true;
        if (rate < 1) return false;
        return (level.getGameTime() + pos.hashCode()) % rate == 0;
    }

    public static String categoryKey(MobCategory category) {
        return category.getName().toLowerCase(java.util.Locale.ROOT);
    }

    public static int paperGlobalInt(String path, int fallback) {
        try {
            return org.bukkit.Bukkit.getServer().spigot().getPaperConfig().getInt(path, fallback);
        } catch (Throwable unavailable) {
            return fallback;
        }
    }

    public static boolean paperGlobalBoolean(String path, boolean fallback) {
        try {
            return org.bukkit.Bukkit.getServer().spigot().getPaperConfig().getBoolean(path, fallback);
        } catch (Throwable unavailable) {
            return fallback;
        }
    }

    public static String paperGlobalString(String path, String fallback) {
        try {
            return org.bukkit.Bukkit.getServer().spigot().getPaperConfig().getString(path, fallback);
        } catch (Throwable unavailable) {
            return fallback;
        }
    }

    private static final Map<String, Double> NUMBERS = new ConcurrentHashMap<>();
    private static final Map<Block, String> GROWTH_KEYS = new IdentityHashMap<>();
    private static volatile Boolean growthActive;


    public static YamlConfiguration spigot() {
        try {
            return Bukkit.getServer().spigot().getSpigotConfig();
        } catch (Throwable unavailable) {
            return null;
        }
    }

    private static String worldName(Level level) {
        try {
            org.bukkit.World world = LunarArcServerAccess.getCraftWorld(level);
            return world == null ? null : world.getName();
        } catch (Throwable notReady) {
            return null;
        }
    }

    public static double number(Level level, String path, double fallback) {
        String world = worldName(level);
        if (world == null) return fallback;
        String cacheKey = world + "|" + path;
        Double cached = NUMBERS.get(cacheKey);
        if (cached != null) return cached;
        YamlConfiguration spigot = spigot();
        double value = fallback;
        if (spigot != null) {
            String perWorld = "world-settings." + world + "." + path;
            String shared = "world-settings.default." + path;
            if (spigot.isInt(perWorld) || spigot.isDouble(perWorld)) value = spigot.getDouble(perWorld);
            else if (spigot.isInt(shared) || spigot.isDouble(shared)) value = spigot.getDouble(shared);
        }
        NUMBERS.put(cacheKey, value);
        return value;
    }

    public static int intValue(String worldName, String path, int fallback) {
        YamlConfiguration spigot = spigot();
        if (spigot == null) return fallback;
        String perWorld = "world-settings." + worldName + "." + path;
        if (spigot.isInt(perWorld)) return spigot.getInt(perWorld);
        String shared = "world-settings.default." + path;
        return spigot.isInt(shared) ? spigot.getInt(shared) : fallback;
    }

    public static boolean flag(Level level, String path, boolean fallback) {
        String world = worldName(level);
        YamlConfiguration spigot = spigot();
        if (world == null || spigot == null) return fallback;
        String perWorld = "world-settings." + world + "." + path;
        if (spigot.isBoolean(perWorld)) return spigot.getBoolean(perWorld);
        return spigot.getBoolean("world-settings.default." + path, fallback);
    }

    public static void invalidate() {
        NUMBERS.clear();
        growthActive = null;
    }

    public static boolean growthConfigured() {
        Boolean active = growthActive;
        if (active == null) {
            active = scanGrowth();
            growthActive = active;
        }
        return active;
    }

    private static boolean scanGrowth() {
        YamlConfiguration spigot = spigot();
        if (spigot == null) return false;
        ConfigurationSection worlds = spigot.getConfigurationSection("world-settings");
        if (worlds == null) return false;
        for (String world : worlds.getKeys(false)) {
            ConfigurationSection growth = worlds.getConfigurationSection(world + ".growth");
            if (growth == null) continue;
            for (String key : growth.getKeys(false)) {
                if (growth.isInt(key) && growth.getInt(key) != 100) return true;
            }
        }
        return false;
    }

    public static int growthModifier(Level level, Block block) {
        String key;
        synchronized (GROWTH_KEYS) {
            if (!GROWTH_KEYS.containsKey(block)) GROWTH_KEYS.put(block, keyFor(block));
            key = GROWTH_KEYS.get(block);
        }
        if (key == null) return 100;
        return (int) number(level, "growth." + key, 100.0D);
    }

    private static String keyFor(Block block) {
        net.minecraft.resources.ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        if (id == null || !"minecraft".equals(id.getNamespace())) return null;
        String path = id.getPath();
        return switch (path) {
            case "wheat" -> "wheat-modifier";
            case "carrots" -> "carrot-modifier";
            case "potatoes" -> "potato-modifier";
            case "beetroots" -> "beetroot-modifier";
            case "melon_stem" -> "melon-modifier";
            case "pumpkin_stem" -> "pumpkin-modifier";
            case "cactus" -> "cactus-modifier";
            case "sugar_cane" -> "cane-modifier";
            case "nether_wart" -> "wart-modifier";
            case "cocoa" -> "cocoa-modifier";
            case "vine" -> "vine-modifier";
            case "sweet_berry_bush" -> "sweetberry-modifier";
            case "kelp" -> "kelp-modifier";
            case "bamboo", "bamboo_sapling" -> "bamboo-modifier";
            case "red_mushroom", "brown_mushroom" -> "mushroom-modifier";
            default -> path.endsWith("_sapling") ? "sapling-modifier" : null;
        };
    }

    private static final ThreadLocal<ServerLevel> CURRENT = new ThreadLocal<>();


    public static ServerLevel enter(ServerLevel level) {
        ServerLevel previous = CURRENT.get();
        CURRENT.set(level);
        return previous;
    }

    public static void leave(ServerLevel previous) {
        if (previous == null) CURRENT.remove();
        else CURRENT.set(previous);
    }

    private static CraftWorld world() {
        ServerLevel level = CURRENT.get();
        if (level == null) return null;
        try {
            return LunarArcServerAccess.getCraftWorld(level);
        } catch (Throwable notReady) {
            return null;
        }
    }

    public static int limit(MobCategory category, int vanilla) {
        if (category == MobCategory.MISC) return vanilla;
        ServerLevel level = CURRENT.get();
        if (level != null) {
            Integer paper = tuning(level).spawnLimits.get(categoryKey(category));
            if (paper != null) return paper;
        }
        CraftWorld world = world();
        if (world == null) return vanilla;
        org.bukkit.entity.SpawnCategory bukkit = CraftSpawnCategory.toBukkit(category);
        if (bukkit == org.bukkit.entity.SpawnCategory.MISC) return vanilla;
        int limit = world.getSpawnLimit(bukkit);
        return limit < 0 ? vanilla : limit;
    }

    public static boolean perPlayerMobSpawns() {
        ServerLevel level = CURRENT.get();
        return level != null && tuning(level).perPlayerMobSpawns;
    }

    public static long ticksPer(MobCategory category, long vanilla) {
        if (category == MobCategory.MISC) return vanilla;
        CraftWorld world = world();
        if (world == null) return vanilla;
        org.bukkit.entity.SpawnCategory bukkit = CraftSpawnCategory.toBukkit(category);
        if (bukkit == org.bukkit.entity.SpawnCategory.MISC) return vanilla;
        long ticks = world.getTicksPerSpawns(bukkit);
        return ticks < 0 ? vanilla : ticks;
    }

    public static long animalTicks(long vanilla) {
        long ticks = ticksPer(MobCategory.CREATURE, vanilla);
        return ticks == 0 ? Long.MAX_VALUE : ticks;
    }

    public static boolean cadenceAllows(MobCategory category) {
        if (category == MobCategory.CREATURE || category == MobCategory.MISC) return true;
        ServerLevel level = CURRENT.get();
        if (level == null) return true;
        long ticks = ticksPer(category, 1L);
        if (ticks == 1L) return true;
        if (ticks <= 0L) return false;
        return level.getGameTime() % ticks == 0L;
    }
}
