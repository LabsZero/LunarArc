package io.lunararcdevs.lunararc.common.mod.util.remapper.patcher.integrated;

import io.lunararcdevs.lunararc.common.mod.util.remapper.patcher.PluginPatcher;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public final class LunarArcIntegratedPatcher implements PluginPatcher {

    private static final Map<String, BiConsumer<ClassNode, ClassRepo>> SPECIFIC = new HashMap<>();

    // See LunarArcWorldEditRegenLevel for why this exists: WorldEdit's PaperweightAdapter builds
    // a throwaway ServerLevel for //regen through a Paper-only constructor overload this platform
    // doesn't have and Mixin can't add. Retargeting the NEW/INVOKESPECIAL pair to our subclass at
    // plugin-load time keeps WorldEdit's own bytecode otherwise untouched.
    private static final String WORLDEDIT_REGEN_ADAPTER = "com/sk89q/worldedit/bukkit/adapter/impl/v1_21/PaperweightAdapter";
    private static final String SERVER_LEVEL_MOJANG = "net/minecraft/server/level/ServerLevel";
    private static final String REGEN_LEVEL = "io/lunararcdevs/lunararc/common/compat/worldedit/LunarArcWorldEditRegenLevel";
    private static final String WORLDEDIT_NOOP_LISTENER =
            WORLDEDIT_REGEN_ADAPTER + "$NoOpWorldLoadListener";
    private static final String LUNARARC_NOOP_LISTENER =
            "io/lunararcdevs/lunararc/common/compat/worldedit/LunarArcNoOpChunkProgressListener";

    private static final String REGEN_CTOR_DESC_MOJANG =
            "(Lnet/minecraft/server/MinecraftServer;"
                    + "Ljava/util/concurrent/Executor;"
                    + "Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;"
                    + "Lnet/minecraft/world/level/storage/PrimaryLevelData;"
                    + "Lnet/minecraft/resources/ResourceKey;"
                    + "Lnet/minecraft/world/level/dimension/LevelStem;"
                    + "Lnet/minecraft/server/level/progress/ChunkProgressListener;"
                    + "ZJLjava/util/List;ZLnet/minecraft/world/RandomSequences;"
                    + "Lorg/bukkit/World$Environment;"
                    + "Lorg/bukkit/generator/ChunkGenerator;"
                    + "Lorg/bukkit/generator/BiomeProvider;)V";

    private static final String WORLDEDIT_ADAPTER_PREFIX = "com/sk89q/worldedit/bukkit/adapter/impl/";
    private static final String WORLDEDIT_PROPERTY_LOADER_SUFFIX = "/PaperweightAdapter$1";
    private static final String WORLDEDIT_PROPERTY = "com/sk89q/worldedit/registry/state/Property";
    private static final String WORLDEDIT_ENUM_PROPERTY = "com/sk89q/worldedit/registry/state/EnumProperty";
    private static final String WORLDEDIT_PROPERTY_COMPAT =
            "io/lunararcdevs/lunararc/common/mod/util/remapper/patcher/integrated/LunarArcIntegratedPatcher";

    private static final String CLOSE_NAME = "close";
    private static final String CLOSE_WITH_SAVE_DESC = "(Z)V";
    private static final String CLOSE_NO_ARG_DESC = "()V";
    private static final String PLAYER_INFO_PACKET_MOJANG =
            "net/minecraft/network/protocol/game/ClientboundPlayerInfoUpdatePacket";
    private static final String PLAYER_INFO_ENTRY_MOJANG = PLAYER_INFO_PACKET_MOJANG + "$Entry";
    private static final String PLAYER_INFO_COMPAT =
            "io/lunararcdevs/lunararc/common/compat/LunarArcPlayerInfoUpdatePacketCompat";
    private static final String LEVEL_CHUNK_MOJANG = "net/minecraft/world/level/chunk/LevelChunk";
    private static final String BLOCK_POS_MOJANG = "net/minecraft/core/BlockPos";
    private static final String BLOCK_STATE_MOJANG = "net/minecraft/world/level/block/state/BlockState";

    static {
        SPECIFIC.put(WORLDEDIT_REGEN_ADAPTER, LunarArcIntegratedPatcher::patchWorldEditRegen);
    }

    @Override
    public void handleClass(ClassNode node, ClassRepo classRepo) {
        BiConsumer<ClassNode, ClassRepo> consumer = SPECIFIC.get(node.name);
        if (consumer != null) {
            consumer.accept(node, classRepo);
        }
        patchPlayerInfoUpdateSingleEntryConstructor(node);
        patchChunkSetBlockStateExtraFlag(node);
        if (node.name.startsWith(WORLDEDIT_ADAPTER_PREFIX) && node.name.endsWith(WORLDEDIT_PROPERTY_LOADER_SUFFIX)) {
            patchWorldEditUnknownProperties(node);
        }
    }

    public static java.util.List<String> worldEditPropertyValueNames(Object property) {
        return propertyValueNames((net.minecraft.world.level.block.state.properties.Property<?>) property);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static java.util.List<String> propertyValueNames(net.minecraft.world.level.block.state.properties.Property property) {
        java.util.List<String> names = new java.util.ArrayList<>();
        for (Object value : property.getPossibleValues()) {
            names.add(property.getName((Comparable) value));
        }
        return names;
    }

    private static void patchWorldEditUnknownProperties(ClassNode node) {
        for (MethodNode method : node.methods) {
            if (!"load".equals(method.name) || (method.access & Opcodes.ACC_SYNTHETIC) != 0) continue;
            if (!method.desc.endsWith(")L" + WORLDEDIT_PROPERTY + ";")) continue;

            TypeInsnNode enumNew = null;
            MethodInsnNode enumInit = null;
            MethodInsnNode nameCall = null;
            TypeInsnNode illegalNew = null;
            for (AbstractInsnNode insn : method.instructions) {
                if (insn instanceof TypeInsnNode type && insn.getOpcode() == Opcodes.NEW) {
                    if (type.desc.equals(WORLDEDIT_ENUM_PROPERTY)) enumNew = type;
                    else if (type.desc.equals("java/lang/IllegalArgumentException")) illegalNew = type;
                } else if (insn instanceof MethodInsnNode call) {
                    if (call.getOpcode() == Opcodes.INVOKESPECIAL && call.owner.equals(WORLDEDIT_ENUM_PROPERTY)
                            && "<init>".equals(call.name) && enumInit == null) {
                        enumInit = call;
                    } else if (call.getOpcode() == Opcodes.INVOKEVIRTUAL && "()Ljava/lang/String;".equals(call.desc)
                            && nameCall == null && !call.owner.equals("java/lang/Class")) {
                        nameCall = call;
                    }
                }
            }
            if (enumNew == null || enumInit == null || nameCall == null || illegalNew == null) continue;

            AbstractInsnNode cursor = illegalNew;
            AbstractInsnNode end = null;
            while (cursor != null) {
                if (cursor.getOpcode() == Opcodes.ATHROW) {
                    end = cursor;
                    break;
                }
                cursor = cursor.getNext();
            }
            if (end == null) continue;

            org.objectweb.asm.tree.InsnList replacement = new org.objectweb.asm.tree.InsnList();
            replacement.add(new TypeInsnNode(Opcodes.NEW, WORLDEDIT_ENUM_PROPERTY));
            replacement.add(new org.objectweb.asm.tree.InsnNode(Opcodes.DUP));
            replacement.add(new org.objectweb.asm.tree.VarInsnNode(Opcodes.ALOAD, 1));
            replacement.add(new MethodInsnNode(nameCall.getOpcode(), nameCall.owner, nameCall.name, nameCall.desc, nameCall.itf));
            replacement.add(new org.objectweb.asm.tree.VarInsnNode(Opcodes.ALOAD, 1));
            replacement.add(new MethodInsnNode(Opcodes.INVOKESTATIC, WORLDEDIT_PROPERTY_COMPAT, "worldEditPropertyValueNames",
                    "(Ljava/lang/Object;)Ljava/util/List;", false));
            replacement.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, WORLDEDIT_ENUM_PROPERTY, "<init>", enumInit.desc, false));
            replacement.add(new org.objectweb.asm.tree.InsnNode(Opcodes.ARETURN));

            AbstractInsnNode removeCursor = illegalNew;
            while (removeCursor != null) {
                AbstractInsnNode following = removeCursor.getNext();
                boolean last = removeCursor == end;
                method.instructions.remove(removeCursor);
                if (last) break;
                removeCursor = following;
            }
            method.instructions.add(replacement);
        }
    }

    private static void patchChunkSetBlockStateExtraFlag(ClassNode node) {
        String levelChunkOwner = io.lunararcdevs.lunararc.common.mod.LunarArcRemapper
                .currentRuntimeClassName(LEVEL_CHUNK_MOJANG);
        String blockPosOwner = io.lunararcdevs.lunararc.common.mod.LunarArcRemapper
                .currentRuntimeClassName(BLOCK_POS_MOJANG);
        String blockStateOwner = io.lunararcdevs.lunararc.common.mod.LunarArcRemapper
                .currentRuntimeClassName(BLOCK_STATE_MOJANG);
        String fourArgDesc = "(L" + blockPosOwner + ";L" + blockStateOwner + ";ZZ)L" + blockStateOwner + ";";
        String threeArgDesc = "(L" + blockPosOwner + ";L" + blockStateOwner + ";Z)L" + blockStateOwner + ";";

        for (MethodNode method : node.methods) {
            for (AbstractInsnNode insn : method.instructions) {
                if (!(insn instanceof MethodInsnNode call)) continue;
                if (call.getOpcode() != Opcodes.INVOKEVIRTUAL) continue;
                if (!levelChunkOwner.equals(call.owner) || !fourArgDesc.equals(call.desc)) continue;

                method.instructions.insertBefore(call, new org.objectweb.asm.tree.InsnNode(Opcodes.POP));
                call.desc = threeArgDesc;
            }
        }
    }

    private static void patchPlayerInfoUpdateSingleEntryConstructor(ClassNode node) {
        String packetOwner = io.lunararcdevs.lunararc.common.mod.LunarArcRemapper
                .currentRuntimeClassName(PLAYER_INFO_PACKET_MOJANG);
        String entryOwner = io.lunararcdevs.lunararc.common.mod.LunarArcRemapper
                .currentRuntimeClassName(PLAYER_INFO_ENTRY_MOJANG);
        String ctorDesc = "(Ljava/util/EnumSet;L" + entryOwner + ";)V";
        String vanillaCtorDesc = "(Ljava/util/EnumSet;Ljava/util/Collection;)V";

        for (MethodNode method : node.methods) {
            for (AbstractInsnNode insn : method.instructions) {
                if (!(insn instanceof MethodInsnNode call)) continue;
                if (call.getOpcode() != Opcodes.INVOKESPECIAL || !"<init>".equals(call.name)) continue;
                if (!packetOwner.equals(call.owner) || !ctorDesc.equals(call.desc)) continue;

                // NEW/DUP stay in place so existing stack map frames remain valid.
                method.instructions.insertBefore(call, new MethodInsnNode(Opcodes.INVOKESTATIC, PLAYER_INFO_COMPAT,
                        "stash", "(L" + entryOwner + ";)Ljava/util/Collection;", false));
                call.desc = vanillaCtorDesc;
                method.instructions.insert(call, new MethodInsnNode(Opcodes.INVOKESTATIC, PLAYER_INFO_COMPAT,
                        "apply", "(L" + packetOwner + ";)L" + packetOwner + ";", false));
            }
        }
    }

    @Override
    public String version() {
        return "LunarArc integrated patcher (WorldEdit //regen bridge)";
    }

    private static void patchWorldEditRegen(ClassNode node, ClassRepo classRepo) {
        String serverLevel = io.lunararcdevs.lunararc.common.mod.LunarArcRemapper.currentRuntimeClassName(SERVER_LEVEL_MOJANG);
        String regenCtorDesc = io.lunararcdevs.lunararc.common.mod.LunarArcRemapper.currentRuntimeDescriptor(REGEN_CTOR_DESC_MOJANG);
        for (MethodNode method : node.methods) {
            for (AbstractInsnNode insn : method.instructions) {
                if (!(insn instanceof MethodInsnNode call)) continue;

                if (call.getOpcode() == Opcodes.INVOKESPECIAL && "<init>".equals(call.name)) {
                    String replacement;
                    if (serverLevel.equals(call.owner) && regenCtorDesc.equals(call.desc)) {
                        replacement = REGEN_LEVEL;
                    } else if (WORLDEDIT_NOOP_LISTENER.equals(call.owner) && "()V".equals(call.desc)) {
                        replacement = LUNARARC_NOOP_LISTENER;
                    } else {
                        continue;
                    }

                    TypeInsnNode newInsn = findMatchingNew(call);
                    if (newInsn == null) continue;
                    newInsn.desc = replacement;
                    call.owner = replacement;
                    continue;
                }

                if ((call.getOpcode() == Opcodes.INVOKEVIRTUAL || call.getOpcode() == Opcodes.INVOKEINTERFACE)
                        && CLOSE_NAME.equals(call.name) && CLOSE_WITH_SAVE_DESC.equals(call.desc)) {
                    AbstractInsnNode argPush = previousReal(call);
                    if (argPush == null || (argPush.getOpcode() != Opcodes.ICONST_0 && argPush.getOpcode() != Opcodes.ICONST_1)) {
                        continue;
                    }
                    method.instructions.remove(argPush);
                    call.desc = CLOSE_NO_ARG_DESC;
                }
            }
        }
    }

    private static AbstractInsnNode previousReal(AbstractInsnNode insn) {
        AbstractInsnNode current = insn.getPrevious();
        while (current != null && current.getOpcode() < 0) current = current.getPrevious();
        return current;
    }

    /**
     * Walks backward from a constructor call to the NEW instruction that allocated its receiver,
     * treating every other complete NEW/INVOKESPECIAL<init> pair encountered along the way as a
     * balanced, already-closed argument expression (e.g. a nested {@code new LevelStem(...)}) to
     * skip over rather than mistake for our own - simple bracket-style matching, valid because
     * javac never emits an unmatched NEW.
     */
    private static TypeInsnNode findMatchingNew(MethodInsnNode ctorCall) {
        int pending = 0;
        for (AbstractInsnNode current = ctorCall.getPrevious(); current != null; current = current.getPrevious()) {
            if (current instanceof MethodInsnNode m && current.getOpcode() == Opcodes.INVOKESPECIAL
                    && "<init>".equals(m.name)) {
                pending++;
            } else if (current instanceof TypeInsnNode t && current.getOpcode() == Opcodes.NEW) {
                if (pending == 0) return t;
                pending--;
            }
        }
        return null;
    }
}
