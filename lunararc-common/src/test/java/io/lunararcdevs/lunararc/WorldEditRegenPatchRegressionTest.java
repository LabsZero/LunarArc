package io.lunararcdevs.lunararc;

import io.lunararcdevs.lunararc.common.mod.util.remapper.patcher.integrated.LunarArcIntegratedPatcher;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public final class WorldEditRegenPatchRegressionTest {
    private static final String CTOR_26 = "(Lnet/minecraft/server/MinecraftServer;Ljava/util/concurrent/Executor;"
            + "Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;"
            + "Lnet/minecraft/world/level/levelgen/WorldGenSettings;Lnet/minecraft/resources/ResourceKey;"
            + "Lnet/minecraft/world/level/dimension/LevelStem;ZJLjava/util/List;ZLnet/minecraft/resources/ResourceKey;"
            + "Lorg/bukkit/World$Environment;Lorg/bukkit/generator/ChunkGenerator;Lorg/bukkit/generator/BiomeProvider;"
            + "Lnet/minecraft/world/level/storage/SavedDataStorage;Lio/papermc/paper/world/PaperWorldLoader$LoadedWorldData;)V";
    private static final String REGEN = "io/lunararcdevs/lunararc/common/compat/worldedit/LunarArcWorldEditRegenLevel";

    public static void run() {
        for (String adapter : new String[]{"v26_3", "v1_21_11"}) {
            String name = "com/sk89q/worldedit/bukkit/adapter/impl/" + adapter + "/PaperweightAdapter";
            ClassWriter writer = new ClassWriter(0);
            writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, name, null, "java/lang/Object", null);
            var method = writer.visitMethod(Opcodes.ACC_PUBLIC, "doRegen", "()V", null, null);
            method.visitCode();
            method.visitTypeInsn(Opcodes.NEW, "net/minecraft/server/level/ServerLevel");
            method.visitInsn(Opcodes.DUP);
            // the patcher only inspects the constructor call's owner and descriptor, not its arguments
            method.visitMethodInsn(Opcodes.INVOKESPECIAL, "net/minecraft/server/level/ServerLevel", "<init>",
                    adapter.equals("v26_3") ? CTOR_26 : "()V", false);
            method.visitInsn(Opcodes.POP);
            method.visitInsn(Opcodes.RETURN);
            method.visitMaxs(0, 1);
            method.visitEnd();
            writer.visitEnd();

            ClassNode node = new ClassNode();
            new ClassReader(writer.toByteArray()).accept(node, 0);
            new LunarArcIntegratedPatcher().handleClass(node, null);

            boolean redirected = false;
            for (MethodNode m : node.methods) {
                for (AbstractInsnNode insn : m.instructions) {
                    if (insn instanceof MethodInsnNode call && "<init>".equals(call.name) && REGEN.equals(call.owner)) redirected = true;
                }
            }
            if (adapter.equals("v26_3") && !redirected) {
                throw new AssertionError("WorldEdit v26_3 //regen ServerLevel constructor was not redirected to the LunarArc regen level");
            }
        }
        checkSetValueRedirect();
        System.out.println("WorldEdit regen patch regressions passed");
    }

    private static void checkSetValueRedirect() {
        String name = "com/sk89q/worldedit/bukkit/adapter/impl/v26_3/PaperweightAdapter";
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, name, null, "java/lang/Object", null);
        var method = writer.visitMethod(Opcodes.ACC_PUBLIC, "apply", "()V", null, null);
        method.visitCode();
        method.visitInsn(Opcodes.ACONST_NULL);
        method.visitInsn(Opcodes.ACONST_NULL);
        method.visitInsn(Opcodes.ACONST_NULL);
        method.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "net/minecraft/world/level/block/state/BlockState", "setValue",
                "(Lnet/minecraft/world/level/block/state/properties/Property;Ljava/lang/Comparable;)Ljava/lang/Object;", false);
        method.visitInsn(Opcodes.POP);
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(0, 1);
        method.visitEnd();
        writer.visitEnd();
        ClassNode node = new ClassNode();
        new ClassReader(writer.toByteArray()).accept(node, 0);
        new LunarArcIntegratedPatcher().handleClass(node, null);
        boolean redirected = false;
        for (MethodNode m : node.methods) {
            for (AbstractInsnNode insn : m.instructions) {
                if (insn instanceof MethodInsnNode call && "worldEditSetValue".equals(call.name)) redirected = true;
            }
        }
        if (!redirected) throw new AssertionError("WorldEdit setValue was not routed through the property-name helper");
    }
}
