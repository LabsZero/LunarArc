package io.ampznetwork.lunararc.common.server;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.function.Consumer;

public final class LunarArcPluginFixManager {

    private static final String REPLACEMENT = "lunararc";

    private LunarArcPluginFixManager() {}

    /**
     * Applies the per-plugin settings that have to be in place before a plugin's first class is
     * even loaded, keyed on the plugin's name rather than on any one class.
     *
     * <p>Called from the plugin's classloader constructor rather than from
     * {@link #injectPluginFix(String, byte[])}. MohistMC/Youer does this from inside its
     * class-transform hook, which works there because Youer transforms every class on every
     * start; LunarArc caches transformed classes on disk, so on the second and every later start
     * the transform is skipped entirely and a side effect living inside it would simply never
     * happen. Anchoring it to classloader construction makes it happen exactly once per plugin
     * per start, cache or no cache.</p>
     */
    public static void applyPluginProperties(String pluginName) {
        if (pluginName == null) return;

        if (pluginName.equals("WorldEdit")) {
            // WorldEdit picks its NMS adapter through BukkitImplLoader, which identifies the
            // server it is running on before choosing. On a hybrid that identification does not
            // land, no adapter is selected, and WorldEdit disables itself during onEnable - after
            // which every later reference to one of its classes throws NoClassDefFoundError from
            // the closed plugin classloader, thousands of times, which looks like the fault
            // rather than the aftermath of it.
            //
            // BukkitImplLoader consults this system property first and loads the named adapter
            // directly, skipping detection. Same value and same only-if-unset guard as Youer, so
            // FastAsyncWorldEdit - which sets its own fawe adapter under a different plugin name -
            // keeps whatever it chose.
            if (System.getProperty("worldedit.bukkit.adapter") == null) {
                System.setProperty("worldedit.bukkit.adapter",
                        "com.sk89q.worldedit.bukkit.adapter.impl.v1_21.PaperweightAdapter");
            }
        }
    }

    public static byte[] injectPluginFix(String className, byte[] clazz) {
        Consumer<ClassNode> patcher = switch (className) {
            case "com.sk89q.worldedit.bukkit.BukkitConfiguration" -> node -> {
                helloWorld(node, "I accept that I will receive no support with this flag enabled.", REPLACEMENT);
                helloWorld(node, "allow-editing-on-unsupported-versions", REPLACEMENT);
                helloWorld(node, "false", REPLACEMENT);
            };
            case "com.sk89q.worldedit.bukkit.adapter.impl.v1_21.PaperweightAdapter",
                 "com.sk89q.worldedit.bukkit.adapter.ext.fawe.v1_21_R1.PaperweightAdapter" ->
                    node -> helloWorld(node, "org.spigotmc.WatchdogThread", REPLACEMENT);
            case "com.sk89q.worldedit.bukkit.paperlib.PaperLib" -> node -> {
                removePaper0(node);
                if (System.getProperty("paperlib.shown-benefits") == null) {
                    System.setProperty("paperlib.shown-benefits", "1");
                }
            };
            case "org.mvplugins.multiverse.external.paperlib.PaperLib",
                 "me.SuperRonanCraft.BetterRTP.lib.paperlib.PaperLib",
                 "com.plotsquared.bukkit.paperlib.PaperLib" ->
                    LunarArcPluginFixManager::removePaper0;
            case "com.fastasyncworldedit.bukkit.util.MinecraftVersion" ->
                    node -> redirectMethodToGetNMSVersion(node, "getPackageVersion");
            case "com.ghostchu.quickshop.platform.spigot.AbstractSpigotPlatform" ->
                    node -> redirectMethodToGetNMSVersion(node, "getNMSVersion");
            default -> null;
        };
        return patcher == null ? clazz : patch(clazz, patcher);
    }

    private static void removePaper0(ClassNode node) {
        helloWorld(node, "com.destroystokyo.paper.PaperConfig", REPLACEMENT);
        helloWorld(node, "io.papermc.paper.configuration.Configuration", REPLACEMENT);
    }

    private static void redirectMethodToGetNMSVersion(ClassNode node, String methodName) {
        for (MethodNode methodNode : node.methods) {
            if (methodNode.name.equals(methodName) && methodNode.desc.equals("()Ljava/lang/String;")) {
                InsnList toInject = new InsnList();
                toInject.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        Type.getInternalName(LunarArcPluginFixManager.class),
                        "getNMSVersion",
                        "()Ljava/lang/String;"));
                toInject.add(new InsnNode(Opcodes.ARETURN));
                methodNode.instructions = toInject;
                methodNode.tryCatchBlocks.clear();
            }
        }
    }

    public static String getNMSVersion() {
        return "v1_21_R1";
    }

    private static byte[] patch(byte[] basicClass, Consumer<ClassNode> handler) {
        org.objectweb.asm.ClassReader reader = new org.objectweb.asm.ClassReader(basicClass);
        ClassNode node = new ClassNode();
        reader.accept(node, 0);
        handler.accept(node);
        org.objectweb.asm.ClassWriter writer = new org.objectweb.asm.ClassWriter(0);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static void helloWorld(ClassNode node, String a, String b) {
        node.methods.forEach(method -> {
            for (AbstractInsnNode next : method.instructions) {
                if (next instanceof LdcInsnNode ldcInsnNode) {
                    if (ldcInsnNode.cst instanceof String str) {
                        if (a.equals(str)) {
                            ldcInsnNode.cst = b;
                        }
                    }
                }
            }
        });
    }
}
