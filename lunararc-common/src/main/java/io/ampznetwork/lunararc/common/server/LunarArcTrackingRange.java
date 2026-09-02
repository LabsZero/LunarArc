package io.ampznetwork.lunararc.common.server;

import io.ampznetwork.lunararc.common.LunarArcServerAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.raid.Raider;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * How far away an entity is still sent to a player, per Spigot's entity-tracking-range.
 *
 * <p>Vanilla tracks most mobs out to 128 blocks. Spigot lowers that per category, because tracking
 * an entity is not free: every tracked entity is a per-tick distance check and a stream of movement
 * packets to every player in range. On a busy world the difference is a large part of what the
 * server spends its tick on, and it is the reason a Bukkit server feels different from a vanilla
 * one at the same player count. Nothing here changes what exists or how it behaves - only how far
 * away a client is told about it.</p>
 *
 * <p>Written in LunarArc's own namespace rather than as {@code org.spigotmc.TrackingRange}. That
 * class arrives compiled from the Paper jar and reads {@code entity.activationType} and
 * {@code level.spigotConfig}, two fields Spigot adds to Minecraft classes that this server's
 * loader-owned Minecraft does not have; calling it would throw NoSuchFieldError on the first
 * entity. The categories below are the same ones, decided by type instead of by a field. It is
 * server-internal either way - no plugin calls TrackingRange - so nothing is lost by not borrowing
 * the name.</p>
 *
 * <p>Values come from spigot.yml, per world, falling back to the {@code default} section and then
 * to Spigot's own defaults:</p>
 *
 * <pre>
 *   world-settings:
 *     default:
 *       entity-tracking-range:
 *         players: 48
 *         animals: 48
 *         monsters: 48
 *         misc: 32
 *         display: 128
 *         other: 64
 * </pre>
 */
public final class LunarArcTrackingRange {

    private static final Logger LOGGER = LoggerFactory.getLogger("LunarArc");

    /** Spigot's own defaults, in blocks. */
    private static final int DEFAULT_PLAYERS = 48;
    private static final int DEFAULT_ANIMALS = 48;
    private static final int DEFAULT_MONSTERS = 48;
    private static final int DEFAULT_MISC = 32;
    private static final int DEFAULT_DISPLAY = 128;
    private static final int DEFAULT_OTHER = 64;

    private static final Map<String, Ranges> BY_WORLD = new ConcurrentHashMap<>();
    private static volatile boolean announced;

    private record Ranges(int players, int animals, int monsters, int misc, int display, int other) {}

    private LunarArcTrackingRange() {
    }

    /**
     * The range to track this entity at, or {@code defaultRange} where vanilla should decide.
     *
     * @param defaultRange what vanilla worked out, already in blocks
     */
    public static int getEntityTrackingRange(Entity entity, int defaultRange) {
        // Vanilla uses zero for entities it does not track at all. Handing those a range would
        // start tracking something that is meant to be invisible to the client.
        if (defaultRange == 0 || entity == null) return defaultRange;
        // The dragon is deliberately left alone: its fight spans more than any of these ranges, and
        // a dragon that vanishes mid-fight is a far worse trade than the tracking it costs.
        if (entity instanceof EnderDragon) return defaultRange;

        Ranges ranges = rangesFor(entity);
        if (ranges == null) return defaultRange;

        if (entity instanceof ServerPlayer) return ranges.players();
        // Raider extends Monster, so the monster test covers both of Spigot's categories.
        if (entity instanceof Monster || entity instanceof Raider) return ranges.monsters();
        if (entity instanceof Animal || entity instanceof WaterAnimal || entity instanceof AmbientCreature) {
            return ranges.animals();
        }
        if (entity instanceof ItemFrame || entity instanceof Painting
                || entity instanceof ItemEntity || entity instanceof ExperienceOrb) {
            return ranges.misc();
        }
        if (entity instanceof Display) return ranges.display();
        return ranges.other();
    }

    private static Ranges rangesFor(Entity entity) {
        org.bukkit.World world;
        try {
            world = LunarArcServerAccess.getCraftWorld(entity.level());
        } catch (Throwable notReady) {
            // Before the Bukkit layer exists, or for a level with no Bukkit world, vanilla decides.
            return null;
        }
        if (world == null) return null;
        return BY_WORLD.computeIfAbsent(world.getName(), LunarArcTrackingRange::read);
    }

    private static Ranges read(String worldName) {
        YamlConfiguration spigot = spigotConfig();
        Ranges ranges = new Ranges(
                value(spigot, worldName, "players", DEFAULT_PLAYERS),
                value(spigot, worldName, "animals", DEFAULT_ANIMALS),
                value(spigot, worldName, "monsters", DEFAULT_MONSTERS),
                value(spigot, worldName, "misc", DEFAULT_MISC),
                value(spigot, worldName, "display", DEFAULT_DISPLAY),
                value(spigot, worldName, "other", DEFAULT_OTHER));
        if (!announced) {
            announced = true;
            LOGGER.info("Entity tracking ranges in effect (blocks): players {}, animals {}, monsters {}, "
                    + "misc {}, display {}, other {}. Vanilla tracks most entities to 128; set them under "
                    + "world-settings.<world>.entity-tracking-range in spigot.yml.",
                    ranges.players(), ranges.animals(), ranges.monsters(),
                    ranges.misc(), ranges.display(), ranges.other());
        }
        return ranges;
    }

    private static int value(YamlConfiguration spigot, String worldName, String key, int fallback) {
        if (spigot == null) return fallback;
        String perWorld = "world-settings." + worldName + ".entity-tracking-range." + key;
        if (spigot.isInt(perWorld)) return spigot.getInt(perWorld);
        String shared = "world-settings.default.entity-tracking-range." + key;
        if (spigot.isInt(shared)) return spigot.getInt(shared);
        return fallback;
    }

    private static YamlConfiguration spigotConfig() {
        try {
            return Bukkit.getServer().spigot().getSpigotConfig();
        } catch (Throwable unavailable) {
            return null;
        }
    }

    /** Forget cached values, so a reload picks up an edited spigot.yml. */
    public static void invalidate() {
        BY_WORLD.clear();
    }
}
