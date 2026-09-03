package io.papermc.paper.util;

import net.minecraft.world.entity.MobCategory;

import java.io.InputStream;
import java.util.Objects;

/**
 * Describes the mapping shape of the running Minecraft runtime.
 *
 * <p>This is LunarArc's own implementation of a class real Paper keeps in its server jar. It is
 * needed because the exact Paper class is not part of LunarArc's donor boundary - {@code
 * preparePaperImplementationSurface} only donates {@code org/bukkit/craftbukkit/**}, {@code
 * org/spigotmc/**} and {@code io/papermc/paper/configuration/**} - while a class that <em>is</em>
 * donated, {@link org.bukkit.craftbukkit.util.Commodore}, reads {@link #LEGACY_CB_VERSION},
 * {@link #reobf()} and {@link #hasMappings()} from it. Without an implementation on this exact
 * name Commodore's class initialization fails with NoClassDefFoundError.</p>
 *
 * <p>The answers differ from Paper's for a reason. Paper ships one jar that can be either
 * Mojang-mapped or reobfuscated, so it has to detect which it is and may carry a bundled
 * {@code META-INF/mappings/reobf.tiny}. On LunarArc the Minecraft runtime is always the loader's -
 * NeoForge, Forge, Fabric or Quilt - and all four are Mojang-mapped for 1.21.1, so {@link
 * #reobf()} is answered by the same probe Paper uses rather than assumed, and LunarArc ships no
 * {@code reobf.tiny} at Paper's resource path (its own Spigot member mappings live at
 * {@code mappings/&lt;version&gt;/paper-reobf.tiny} and are consumed by LunarArcRemapper, not here).</p>
 *
 * <p>Both answers being {@code false} is what Commodore wants: it then rewrites the legacy
 * {@code org/bukkit/craftbukkit/v1_21_R1/} package prefix down to the unversioned one itself.
 * LunarArcRemapper performs the same relocation, so the two agree and the second pass is a no-op -
 * this helps Paper's transform rather than competing with it.</p>
 */
public final class MappingEnvironment {

    /** Kill switch Paper honours for its plugin remapper; mirrored so the same flag works here. */
    public static final boolean DISABLE_PLUGIN_REMAPPING = Boolean.getBoolean("paper.disablePluginRemapping");

    /**
     * The versioned CraftBukkit package Spigot-era plugins were compiled against on 1.21.1.
     * Commodore builds {@code org/bukkit/craftbukkit/v1_21_R1/} from this to strip it.
     */
    public static final String LEGACY_CB_VERSION = "v1_21_R1";

    private static final boolean REOBF = checkReobf();

    private MappingEnvironment() {
    }

    /** {@code true} only if the running Minecraft is reobfuscated (Spigot-mapped). */
    public static boolean reobf() {
        return REOBF;
    }

    /** {@code true} if a Paper-style bundled reobf mapping file is on the classpath. */
    public static boolean hasMappings() {
        return mappingsStreamIfPresent() != null;
    }

    public static InputStream mappingsStream() {
        return Objects.requireNonNull(mappingsStreamIfPresent(), "Missing mappings!");
    }

    public static InputStream mappingsStreamIfPresent() {
        return MappingEnvironment.class.getClassLoader().getResourceAsStream("META-INF/mappings/reobf.tiny");
    }

    // Paper's own probe: on a Mojang-mapped runtime the class keeps its readable name, on a
    // reobfuscated one it carries the old Spigot name. Works identically on all four loaders
    // because it asks the loaded class rather than the environment it was loaded from.
    private static boolean checkReobf() {
        String name = MobCategory.class.getSimpleName();
        if (name.equals("MobCategory")) return false;
        if (name.equals("EnumCreatureType")) return true;
        throw new IllegalStateException("Unable to determine mapping environment from MobCategory: " + name);
    }
}
