package io.papermc.paper.pluginremap.reflect;

import io.papermc.paper.util.MappingEnvironment;
import org.objectweb.asm.ClassVisitor;

/**
 * Rewrites reflective references to Minecraft symbols in plugin bytecode.
 *
 * <p>Like {@link MappingEnvironment}, this class exists on this exact name because a donated Paper
 * class needs it: {@link org.bukkit.craftbukkit.util.Commodore} wraps its output visitor in
 * {@link #visitor(ClassVisitor)}, and Paper's own implementation lives outside LunarArc's donor
 * boundary. Without it Commodore cannot link.</p>
 *
 * <p>The behaviour here is a deliberate passthrough, and it is the same behaviour real Paper has
 * on this runtime. Paper's implementation exists to repair plugins that reflect on
 * <em>obfuscated</em> Spigot symbol names while the server runs Mojang-mapped classes, and it
 * disables itself when the runtime is not reobfuscated. LunarArc's Minecraft is always the
 * loader's - NeoForge, Forge, Fabric or Quilt - and all four are Mojang-mapped for 1.21.1, so
 * {@link MappingEnvironment#reobf()} is {@code false} and Paper would be a no-op here too.</p>
 *
 * <p>Reflective symbol repair on the classic Spigot plugin path is handled separately by
 * LunarArcRemapper and its {@code plugin-remap.tsv} rules, which run over the same bytecode after
 * Commodore. Duplicating that work inside Commodore's visitor chain would risk rewriting a symbol
 * twice, so the two stay in their own lanes: Commodore does Paper's API-level rerouting, and
 * LunarArc's remapper does the hybrid NMS work.</p>
 */
public final class ReflectionRemapper {

    private ReflectionRemapper() {
    }

    /** Returns {@code parent} unchanged unless the runtime is reobfuscated, which it never is here. */
    public static ClassVisitor visitor(ClassVisitor parent) {
        return parent;
    }

    /** Returns {@code bytecode} unchanged; see the class javadoc for why there is nothing to do. */
    public static byte[] processClass(byte[] bytecode) {
        return bytecode;
    }

    /** Whether this remapper would do anything on the current runtime. */
    public static boolean enabled() {
        return !MappingEnvironment.DISABLE_PLUGIN_REMAPPING && MappingEnvironment.reobf();
    }
}
