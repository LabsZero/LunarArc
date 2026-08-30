package io.ampznetwork.lunararc.common.mod.util.remapper.patcher.integrated;

import io.ampznetwork.lunararc.common.mod.util.remapper.patcher.PluginPatcher;
import org.objectweb.asm.tree.ClassNode;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Modeled on the real Arclight {@code IntegratedPatcher}: a registry of known, specific
 * bytecode incompatibilities between bundled third-party plugin classes and LunarArc's
 * CraftBukkit-layer, keyed by the exact internal class name each patch targets.
 * <p>
 * Deliberately empty right now. Arclight's own WorldEdit entries (rewriting a
 * {@code CraftBlockData.createData} call to {@code fromData}, and re-deriving a stale hardcoded
 * NMS field name in {@code StaticRefraction}'s {@code <clinit>}) are calibrated to Arclight's
 * specific remapper/CraftBukkit-layer naming — LunarArc's actual incompatibilities, if any, are
 * unknown until we have the real swallowed exception from WorldEdit's adapter loader (its
 * {@code BukkitImplLoader.loadAdapter()} silently discards the failure reason unless
 * {@code -Dworldedit.bukkit.adapter=<class>} names the candidate explicitly). Add real entries
 * here once that's in hand — do not guess at replacement values.
 */
public final class LunarArcIntegratedPatcher implements PluginPatcher {

    private static final Map<String, BiConsumer<ClassNode, ClassRepo>> SPECIFIC = new HashMap<>();

    @Override
    public void handleClass(ClassNode node, ClassRepo classRepo) {
        BiConsumer<ClassNode, ClassRepo> consumer = SPECIFIC.get(node.name);
        if (consumer != null) {
            consumer.accept(node, classRepo);
        }
    }

    @Override
    public String version() {
        return "LunarArc integrated patcher (no entries registered yet)";
    }
}
