package io.lunararcdevs.lunararc.common.mixinplugin;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class LunarArcMixinConfigPlugin implements IMixinConfigPlugin {

    private static final String MINECRAFT_SERVER = "net/minecraft/server/MinecraftServer";
    private static final String SERVER_ACCESS =
            "io/lunararcdevs/lunararc/common/LunarArcServerAccess";
    private static final String GET_SERVER_DESC = "()L" + MINECRAFT_SERVER + ";";

    private static final String PLAYER_INFO_PACKET =
            "net/minecraft/network/protocol/game/ClientboundPlayerInfoUpdatePacket";
    private static final String PLAYER_INFO_ENTRY = PLAYER_INFO_PACKET + "$Entry";
    // The offline dev-reference jar (minecraft-merged-srg-patched.jar) uses SRG names
    // ("f_244436_"), but a live boot proved the actual runtime class this hook receives keeps
    // the real Mojang name "entries" - confirmed via a real NoSuchFieldError when f_244436_
    // was used here ("does not have member field 'java.util.List f_244436_'").
    private static final String ENTRIES_FIELD = "entries";

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        // Forge's patched AbstractFurnaceBlockEntity#burn is an instance method, so Forge ships its own smelt-event handler.
        if (mixinClassName.endsWith(".FurnaceSmeltEventMixin") && isForge()) return false;
        return true;
    }

    private static Boolean forge;

    private static boolean isForge() {
        if (forge == null) {
            forge = LunarArcMixinConfigPlugin.class.getClassLoader().getResource("net/minecraftforge/fml/ModList.class") != null;
        }
        return forge;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        // Nothing here may throw. This runs inside mixin application, where an escaping exception
        // is not a failed feature but a server that never starts.
        try {
            if (MINECRAFT_SERVER.equals(targetClass.name)) {
                addStaticGetServer(targetClass);
            } else if (PLAYER_INFO_PACKET.equals(targetClass.name)) {
                addSingleEntryConstructor(targetClass);
            }
            addPaperStatics(targetClass);
            addPaperStaticFields(targetClass);
        } catch (Throwable ignored) {
            // A missing accessor is a plugin-compatibility gap; a thrown one is a dead server.
        }
    }

    private static final String STATICS = "io/lunararcdevs/lunararc/common/compat/PaperStatics";
    private static final String[][] PAPER_STATICS = {
            // target class, method name, descriptor, PaperStatics method
            {"net/minecraft/world/entity/decoration/ItemFrame", "createBoundingBoxStatic",
                    "(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Z)Lnet/minecraft/world/phys/AABB;", "itemFrameBox"},
            {"net/minecraft/world/entity/decoration/painting/Painting", "calculateBoundingBoxStatic",
                    "(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;II)Lnet/minecraft/world/phys/AABB;", "paintingBox"},
            {"net/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity", "getTotalCookTime",
                    "(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;"
                            + "Lnet/minecraft/world/item/crafting/RecipeType;D)I", "totalCookTime"},
            {"net/minecraft/world/level/block/entity/ConduitBlockEntity", "updateAndAttackTarget",
                    "(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;"
                            + "Lnet/minecraft/world/level/block/entity/ConduitBlockEntity;ZZ)V", "conduitUpdate"},
            {"net/minecraft/world/level/storage/TagValueInput", "createGlobal",
                    "(Lnet/minecraft/util/ProblemReporter;Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/world/level/storage/ValueInput;", "createGlobal"},
    };

    // target class, field name, descriptor, PaperStatics method producing its initial value
    private static final String[][] PAPER_STATIC_FIELDS = {
            {"net/minecraft/world/item/component/AttackRange", "CODEC_DEFAULT", "Lnet/minecraft/world/item/component/AttackRange;", "attackRangeDefault"},
    };

    /**
     * Declares the public statics Paper patches into vanilla, as thin delegates to {@code PaperStatics}. A mixin may not
     * declare public statics, so they are added to the target class directly.
     */
    private static void addPaperStatics(ClassNode targetClass) {
        for (String[] entry : PAPER_STATICS) {
            if (!entry[0].equals(targetClass.name)) continue;
            boolean present = false;
            for (MethodNode existing : targetClass.methods) {
                if (entry[1].equals(existing.name) && entry[2].equals(existing.desc)) present = true;
            }
            if (present) continue;
            MethodNode method = new MethodNode(Opcodes.ASM9, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, entry[1], entry[2], null, null);
            org.objectweb.asm.Type[] args = org.objectweb.asm.Type.getArgumentTypes(entry[2]);
            int slot = 0;
            for (org.objectweb.asm.Type arg : args) {
                method.instructions.add(new VarInsnNode(arg.getOpcode(Opcodes.ILOAD), slot));
                slot += arg.getSize();
            }
            method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, STATICS, entry[3], entry[2], false));
            method.instructions.add(new InsnNode(org.objectweb.asm.Type.getReturnType(entry[2]).getOpcode(Opcodes.IRETURN)));
            method.maxStack = slot + 2;
            method.maxLocals = slot;
            targetClass.methods.add(method);
        }
    }

    /** Adds a public static final field and initialises it at the end of the class initialiser. */
    private static void addPaperStaticFields(ClassNode targetClass) {
        for (String[] entry : PAPER_STATIC_FIELDS) {
            if (!entry[0].equals(targetClass.name)) continue;
            for (org.objectweb.asm.tree.FieldNode existing : targetClass.fields) {
                if (entry[1].equals(existing.name)) return;
            }
            MethodNode clinit = null;
            for (MethodNode method : targetClass.methods) {
                if ("<clinit>".equals(method.name)) clinit = method;
            }
            if (clinit == null) {
                clinit = new MethodNode(Opcodes.ASM9, Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
                clinit.instructions.add(new InsnNode(Opcodes.RETURN));
                clinit.maxStack = 1;
                targetClass.methods.add(clinit);
            }
            targetClass.fields.add(new org.objectweb.asm.tree.FieldNode(Opcodes.ASM9,
                    Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, entry[1], entry[2], null, null));
            org.objectweb.asm.tree.InsnList init = new org.objectweb.asm.tree.InsnList();
            init.add(new MethodInsnNode(Opcodes.INVOKESTATIC, STATICS, entry[3], "()" + entry[2], false));
            init.add(new FieldInsnNode(Opcodes.PUTSTATIC, targetClass.name, entry[1], entry[2]));
            org.objectweb.asm.tree.AbstractInsnNode last = clinit.instructions.getLast();
            while (last != null && last.getOpcode() != Opcodes.RETURN) last = last.getPrevious();
            if (last != null) clinit.instructions.insertBefore(last, init); else clinit.instructions.add(init);
            clinit.maxStack = Math.max(clinit.maxStack, 2);
        }
    }

    private static void addSingleEntryConstructor(ClassNode targetClass) {
        String desc = "(Ljava/util/EnumSet;L" + PLAYER_INFO_ENTRY + ";)V";
        for (MethodNode existing : targetClass.methods) {
            if ("<init>".equals(existing.name) && desc.equals(existing.desc)) return;
        }

        MethodNode ctor = new MethodNode(Opcodes.ASM9, Opcodes.ACC_PUBLIC, "<init>", desc, null, null);
        ctor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        ctor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        ctor.instructions.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC, "java/util/Collections", "emptyList", "()Ljava/util/List;", false));
        ctor.instructions.add(new MethodInsnNode(
                Opcodes.INVOKESPECIAL, PLAYER_INFO_PACKET, "<init>",
                "(Ljava/util/EnumSet;Ljava/util/Collection;)V", false));
        ctor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        ctor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
        ctor.instructions.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC, "java/util/List", "of", "(Ljava/lang/Object;)Ljava/util/List;", true));
        ctor.instructions.add(new FieldInsnNode(
                Opcodes.PUTFIELD, PLAYER_INFO_PACKET, ENTRIES_FIELD, "Ljava/util/List;"));
        ctor.instructions.add(new InsnNode(Opcodes.RETURN));
        ctor.maxStack = 3;
        ctor.maxLocals = 3;
        targetClass.methods.add(ctor);
    }

    /**
     * Declares {@code public static MinecraftServer getServer()}, returning LunarArc's attached
     * server instance - the same thing CraftBukkit's own static returns.
     */
    private static void addStaticGetServer(ClassNode targetClass) {
        // MinecraftServer carries several mixins, so postApply runs more than once for it.
        for (MethodNode existing : targetClass.methods) {
            if ("getServer".equals(existing.name) && GET_SERVER_DESC.equals(existing.desc)) return;
        }

        MethodNode getServer = new MethodNode(
                Opcodes.ASM9,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "getServer",
                GET_SERVER_DESC,
                null,
                null);
        getServer.instructions.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC, SERVER_ACCESS, "getMinecraftServer", GET_SERVER_DESC, false));
        getServer.instructions.add(new InsnNode(Opcodes.ARETURN));
        getServer.maxStack = 1;
        getServer.maxLocals = 0;
        targetClass.methods.add(getServer);
    }

}
