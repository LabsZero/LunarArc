package io.ampznetwork.lunararc.common.mod.util.remapper.patcher;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.tree.ClassNode;

/**
 * Service Provider Interface of plugin patcher.
 * <p>
 * Ported from the real ArclightPowered/api {@code io.izzel.arclight.api.PluginPatcher} —
 * same shape and same declared contract, adapted to LunarArc's own remap pipeline
 * (LunarArcRemapper) instead of Arclight's SpecialSource-based one.
 * <p>
 * Patchers will be loaded from the plugins folder, where in plugin.yml:
 *
 * <pre>
 * lunararc:
 *   patcher: path.to.PatcherClass
 * </pre>
 * <p>
 * The patcher class shall implement {@link PluginPatcher} and expose a no-arg constructor.
 * <p>
 * The patcher shall be loaded before any plugin main class initializes, under a classloader
 * that has access to Minecraft and LunarArc classes.
 * <p>
 * Patchers won't get reloaded or unloaded even if their plugin unloads.
 */
public interface PluginPatcher {

    void handleClass(ClassNode node, ClassRepo classRepo);

    /**
     * Priority of this patcher instance. Lower priority runs first.
     */
    default int priority() {
        return 0;
    }

    /**
     * Returns the version of this plugin patcher.
     * <p>
     * Changes to this version string and/or main class name could invalidate any plugin
     * class cache keyed on patcher identity.
     *
     * @return the patcher version. Defaults to "unknown".
     */
    default String version() {
        String implVersion = getClass().getPackage().getImplementationVersion();
        return implVersion == null ? "unknown" : implVersion;
    }

    interface ClassRepo {

        /**
         * Find a class's bytecode without running the plugin transformer on it — i.e. this
         * must never itself invoke LunarArcRemapper, or a patcher looking up a related class
         * would recurse back into transformation.
         *
         * @param internalName   internal form of the class name (e.g. "com/example/Foo")
         * @param parsingOptions {@link org.objectweb.asm.ClassReader#accept(ClassVisitor, int)}
         * @return class node, or null if nothing is found
         */
        ClassNode findClass(String internalName, int parsingOptions);
    }
}
