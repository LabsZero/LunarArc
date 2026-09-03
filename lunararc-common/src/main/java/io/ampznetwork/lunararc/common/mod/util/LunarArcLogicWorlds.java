package io.ampznetwork.lunararc.common.mod.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Whether a level is a real world Bukkit events should be fired against.
 *
 * <p>Ported from Arclight's {@code DistValidate}, which pins the same Minecraft version we do. It
 * exists because {@code instanceof ServerLevel} is the wrong test on a modded server and every
 * event-firing mixin in this project currently uses it.</p>
 *
 * <p>Mods run vanilla mechanics against levels that are not the world anyone is playing in:
 * simulated worlds for recipe preview and schematic rendering, spatial-storage cells, structure
 * scratch space, single-block fake worlds used to ask a block a question. Most of these are
 * {@code ServerLevel} subclasses, so {@code instanceof} says yes, and LunarArc then fires a Bukkit
 * event naming a block in a world no plugin has ever seen - or asks CraftServer to mint a
 * CraftWorld for it. A plugin that acts on that event acts on nothing, and anything thrown while
 * building it escapes into the vanilla mechanic that was mid-tick.</p>
 *
 * <p>The test is therefore on the exact class, not on assignability. {@code ServerLevel} and
 * {@code WorldGenRegion} are the two vanilla levels where game logic legitimately runs; a subclass
 * is not one of them until someone says it is. Arclight lets its config name extra classes to
 * admit; this keeps the same shape through a system property so an operator hitting a mod that
 * genuinely needs it is not stuck waiting for a build:</p>
 *
 * <pre>
 *   -Dlunararc.logic-worlds=com.example.ModLevel,com.example.OtherLevel
 * </pre>
 *
 * <p>Each unrecognised level class is logged once, with the verdict, so the name to add is in the
 * log rather than something to be guessed at.</p>
 */
public final class LunarArcLogicWorlds {

    private static final Logger LOGGER = LoggerFactory.getLogger("LunarArc");
    private static final Map<Class<?>, Boolean> SEEN = new ConcurrentHashMap<>();
    private static final Set<String> EXTRA = extraLogicWorlds();

    private LunarArcLogicWorlds() {
    }

    /** Whether Bukkit events may be fired against this level. */
    public static boolean isLogicWorld(LevelAccessor level) {
        return level != null && !level.isClientSide() && isLogicClass(level.getClass());
    }

    /** As {@link #isLogicWorld(LevelAccessor)}, for the read-only view vanilla often passes instead. */
    public static boolean isLogicWorld(BlockGetter getter) {
        return getter instanceof LevelAccessor level && isLogicWorld(level);
    }

    private static boolean isLogicClass(Class<?> type) {
        if (type == ServerLevel.class || type == WorldGenRegion.class) return true;
        return SEEN.computeIfAbsent(type, unseen -> {
            boolean admitted = EXTRA.contains(unseen.getName());
            LOGGER.warn("Level class {} treated as a logic world: {}. If a mod's world is wrongly "
                    + "excluded, add it with -Dlunararc.logic-worlds=<class names, comma separated>.",
                    unseen.getName(), admitted);
            return admitted;
        });
    }

    private static Set<String> extraLogicWorlds() {
        String configured = System.getProperty("lunararc.logic-worlds", "");
        Set<String> names = new java.util.HashSet<>();
        for (String part : configured.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) names.add(trimmed);
        }
        return Set.copyOf(names);
    }
}
