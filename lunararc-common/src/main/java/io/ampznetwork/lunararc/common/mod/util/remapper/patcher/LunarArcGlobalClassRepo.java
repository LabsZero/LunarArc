package io.ampznetwork.lunararc.common.mod.util.remapper.patcher;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.service.MixinService;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Modeled on the real Arclight {@code GlobalClassRepo}: reads class bytecode via
 * SpongePowered Mixin's {@code IClassBytecodeProvider} rather than a normal
 * {@code Class.forName}/classloader lookup, specifically because Mixin's bytecode provider
 * returns pre-transform bytes. A patcher (or the redundant safety-net inside LunarArcRemapper
 * itself) looking up another class's structure must never trigger LunarArcRemapper on that
 * class as a side effect, or it would recurse.
 * <p>
 * LunarArc doesn't yet have Arclight's SpecialSource-backed multi-repo setup (in-memory
 * plugin-jar repos, a separate runtime repo, etc.) — this is deliberately the minimal real
 * piece needed for the patcher framework to work: a cache in front of Mixin's provider.
 */
public final class LunarArcGlobalClassRepo implements PluginPatcher.ClassRepo {

    public static final LunarArcGlobalClassRepo INSTANCE = new LunarArcGlobalClassRepo();

    private final ConcurrentMap<String, ClassNode> cache = new ConcurrentHashMap<>();

    private LunarArcGlobalClassRepo() {}

    @Override
    public ClassNode findClass(String internalName, int parsingOptions) {
        if (parsingOptions == ClassReader.SKIP_CODE) {
            return this.cache.computeIfAbsent(internalName, this::findMinecraft);
        }
        // A caller asking for full bytecode (method bodies included) wants a fresh read,
        // not the SKIP_CODE-shaped entry the plain lookup above would cache.
        return findMinecraft(internalName);
    }

    private ClassNode findMinecraft(String internalName) {
        try {
            // Verified against the real Arclight GlobalClassRepo: Mixin's bytecode provider
            // returns a ClassNode directly (pre-transform), not raw bytes.
            return MixinService.getService().getBytecodeProvider().getClassNode(internalName);
        } catch (Exception e) {
            return null;
        }
    }
}
