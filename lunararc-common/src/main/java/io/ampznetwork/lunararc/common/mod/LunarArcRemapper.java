package io.ampznetwork.lunararc.common.mod;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;

import java.util.HashMap;
import java.util.Map;

/**
 * Bytecode remapper for Bukkit/Spigot plugins running on a Paper-based modded server.
 *
 * Placed in the io.ampznetwork.lunararc.common.mod package following the
 * arclight-common convention where mod utilities (remapper, velocity support, …)
 * live under the {@code mod} sub-package while server stubs live under
 * {@code server} and mixin bridges live under {@code bridge}.
 *
 * Responsibilities:
 *  - Remap versioned CraftBukkit class references (e.g. v1_16_R3 → v1_21_R1).
 *  - Remap legacy Spigot/NMS names to their modern Mojang-mapped equivalents so
 *    that plugins compiled against older server jars work alongside mods that use
 *    the current mappings.
 *  - Downgrade bytecode class-file version when a plugin was compiled on a newer
 *    JVM than the one running the server (prevents UnsupportedClassVersionError).
 */
public class LunarArcRemapper extends org.objectweb.asm.commons.Remapper {

    private static final Map<String, String> CLASS_MAP = new HashMap<>();

    // owner -> (srg-name -> mojang-name) for field remapping
    private static final Map<String, Map<String, String>> FIELD_MAP = new HashMap<>();

    static {
        // TAB plugin: ServerPlayer.c (obfuscated) → connection (Mojang)
        Map<String, String> serverPlayerFields = new HashMap<>();
        serverPlayerFields.put("c", "connection");
        serverPlayerFields.put("e", "gameMode");
        FIELD_MAP.put("net/minecraft/server/level/ServerPlayer", serverPlayerFields);

        // ServerGamePacketListenerImpl obfuscated field names
        Map<String, String> listenerFields = new HashMap<>();
        listenerFields.put("a", "player");
        listenerFields.put("b", "connection");
        FIELD_MAP.put("net/minecraft/server/network/ServerGamePacketListenerImpl", listenerFields);
    }

    @Override
    public String mapFieldName(String owner, String name, String descriptor) {
        String mappedOwner = map(owner);
        Map<String, String> fields = FIELD_MAP.get(mappedOwner != null ? mappedOwner : owner);
        if (fields != null) {
            String remapped = fields.get(name);
            if (remapped != null) return remapped;
        }
        return name;
    }

    static {
        // Remap any versioned CraftBukkit package to the current target version.
        for (int i = 13; i <= 21; i++) {
            for (int r = 1; r <= 10; r++) {
                CLASS_MAP.put("org/bukkit/craftbukkit/v1_" + i + "_R" + r,
                        "org/bukkit/craftbukkit/v1_21_R1");
            }
        }

        // Legacy Spigot NMS root package → modern Mojang-mapped package.
        CLASS_MAP.put("net/minecraft/server/v1_21_R1", "net/minecraft/server");

        // Common Spigot → Paper class renames.
        CLASS_MAP.put("net/minecraft/network/NetworkManager",
                "net/minecraft/network/Connection");
        CLASS_MAP.put("net/minecraft/network/protocol/game/PacketPlayOutScoreboardTeam",
                "net/minecraft/network/protocol/game/ClientboundSetPlayerTeamPacket");
        CLASS_MAP.put("net/minecraft/network/protocol/game/PacketPlayOutChat",
                "net/minecraft/network/protocol/game/ClientboundSystemChatPacket");
        CLASS_MAP.put("net/minecraft/network/chat/IChatBaseComponent$ChatSerializer",
                "net/minecraft/network/chat/Component$Serializer");
        CLASS_MAP.put("net/minecraft/network/chat/IChatBaseComponent",
                "net/minecraft/network/chat/Component");

        // Unversioned legacy CraftBukkit references.
        CLASS_MAP.put("org/bukkit/craftbukkit/CraftServer",
                "org/bukkit/craftbukkit/v1_21_R1/CraftServer");
        CLASS_MAP.put("org/bukkit/craftbukkit/entity/CraftPlayer",
                "org/bukkit/craftbukkit/v1_21_R1/entity/CraftPlayer");

        // NMS inner-class renames: obfuscated single-letter nested types → Mojang-mapped names.
        // TAB and similar plugins may reference obfuscated names; NeoForge ships the Mojang-mapped variants.
        // In ClientboundPlayerInfoUpdatePacket the obfuscated $a = Action (enum, has valueOf()),
        // and $b = Entry (record, holds player data).
        CLASS_MAP.put(
                "net/minecraft/network/protocol/game/ClientboundPlayerInfoUpdatePacket$a",
                "net/minecraft/network/protocol/game/ClientboundPlayerInfoUpdatePacket$Action");
        CLASS_MAP.put(
                "net/minecraft/network/protocol/game/ClientboundPlayerInfoUpdatePacket$b",
                "net/minecraft/network/protocol/game/ClientboundPlayerInfoUpdatePacket$Entry");

        // SRG → Mojang-mapped entity / network renames used by TAB and similar plugins.
        CLASS_MAP.put("net/minecraft/server/level/EntityPlayer",
                "net/minecraft/server/level/ServerPlayer");
        CLASS_MAP.put("net/minecraft/server/network/PlayerConnection",
                "net/minecraft/server/network/ServerGamePacketListenerImpl");
        CLASS_MAP.put("net/minecraft/network/protocol/game/PacketPlayInChat",
                "net/minecraft/network/protocol/game/ServerboundChatPacket");
        CLASS_MAP.put("net/minecraft/network/protocol/game/PacketPlayInTabComplete",
                "net/minecraft/network/protocol/game/ServerboundCommandSuggestionPacket");
        CLASS_MAP.put("net/minecraft/network/protocol/game/PacketPlayOutTabComplete",
                "net/minecraft/network/protocol/game/ClientboundCommandSuggestionsPacket");
        CLASS_MAP.put("net/minecraft/network/protocol/game/PacketPlayOutPlayerInfo",
                "net/minecraft/network/protocol/game/ClientboundPlayerInfoUpdatePacket");
        CLASS_MAP.put("net/minecraft/network/protocol/game/PacketPlayOutPlayerListHeaderFooter",
                "net/minecraft/network/protocol/game/ClientboundTabListPacket");
        CLASS_MAP.put("net/minecraft/network/protocol/game/PacketPlayOutTitle",
                "net/minecraft/network/protocol/game/ClientboundSetTitleTextPacket");
        CLASS_MAP.put("net/minecraft/network/protocol/game/PacketPlayOutBoss",
                "net/minecraft/network/protocol/game/ClientboundBossEventPacket");
        CLASS_MAP.put("net/minecraft/network/protocol/game/PacketPlayOutEntityMetadata",
                "net/minecraft/network/protocol/game/ClientboundSetEntityDataPacket");
    }

    @Override
    public String map(String internalName) {
        if (internalName == null) return null;

        // Already the target version — nothing to do.
        if (internalName.startsWith("org/bukkit/craftbukkit/v1_21_R1/")) {
            return internalName;
        }

        // CraftBukkit versioned → current.
        if (internalName.startsWith("org/bukkit/craftbukkit/")) {
            if (internalName.startsWith("org/bukkit/craftbukkit/v1_")) {
                for (Map.Entry<String, String> e : CLASS_MAP.entrySet()) {
                    if (e.getKey().startsWith("org/bukkit/craftbukkit/")
                            && internalName.startsWith(e.getKey())) {
                        return e.getValue() + internalName.substring(e.getKey().length());
                    }
                }
            }
            // Unversioned CraftBukkit reference.
            return "org/bukkit/craftbukkit/v1_21_R1/"
                    + internalName.substring("org/bukkit/craftbukkit/".length());
        }

        // General NMS / library relocations.
        for (Map.Entry<String, String> e : CLASS_MAP.entrySet()) {
            if (internalName.startsWith(e.getKey())) {
                String mapped = e.getValue() + internalName.substring(e.getKey().length());
                if (!mapped.equals(internalName)) return mapped;
            }
        }

        return internalName;
    }

    /**
     * Transforms plugin bytecode: applies name remapping and optionally downgrades
     * the class-file version so a plugin compiled on a newer JDK still loads.
     *
     * @param className internal class name (e.g. "com/sk89q/worldedit/bukkit/BukkitConfiguration")
     */
    public byte[] transform(byte[] bytecode, String className) {
        bytecode = transform(bytecode);
        if (className != null) {
            bytecode = applyWorldEditFixes(bytecode, className);
        }
        return bytecode;
    }

    /** Patch WorldEdit classes to bypass unsupported-version checks. */
    private static byte[] applyWorldEditFixes(byte[] bytecode, String className) {
        switch (className) {
            // Force unsupportedVersionEditing = true by making the config-key lookup return a
            // key that does not exist, so getBoolean() falls back to its default (true when patched).
            case "com/sk89q/worldedit/bukkit/BukkitConfiguration" -> {
                return patchStringConstant(bytecode,
                    "allow-editing-on-unsupported-versions",
                    "lunararc-supported");
            }
            // FastAsyncWorldEdit version probe — make it return our target package version
            case "com/fastasyncworldedit/bukkit/util/MinecraftVersion" -> {
                return patchStringConstant(bytecode, "getPackageVersion", "v1_21_R1");
            }
        }
        return bytecode;
    }

    /** Replace one string constant in all LDC instructions via ASM (safe for any string length). */
    private static byte[] patchStringConstant(byte[] bytes, String from, String to) {
        try {
            ClassReader reader = new ClassReader(bytes);
            ClassWriter writer = new ClassWriter(0);
            reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor,
                        String signature, String[] exceptions) {
                    MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                    return new MethodVisitor(Opcodes.ASM9, mv) {
                        @Override
                        public void visitLdcInsn(Object value) {
                            super.visitLdcInsn(from.equals(value) ? to : value);
                        }
                    };
                }
            }, 0);
            return writer.toByteArray();
        } catch (Throwable t) {
            return bytes;
        }
    }

    /**
     * Transforms plugin bytecode: applies name remapping and optionally downgrades
     * the class-file version so a plugin compiled on a newer JDK still loads.
     */
    public byte[] transform(byte[] bytecode) {
        try {
            if (bytecode.length < 8) return bytecode;

            // Downgrade class-file version if compiled on a newer JVM (max Java 21 = 65).
            int major = ((bytecode[6] & 0xFF) << 8) | (bytecode[7] & 0xFF);
            if (major > 65) {
                bytecode = bytecode.clone();
                bytecode[6] = 0;
                bytecode[7] = 65;
            }

            ClassReader reader = new ClassReader(bytecode);
            ClassWriter writer = new ClassWriter(0);
            ClassRemapper remapper = new ClassRemapper(writer, this);
            reader.accept(remapper, 0);
            return writer.toByteArray();
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().contains("69")) {
                return bytecode; // version-69 quirk — skip silently
            }
            throw e;
        } catch (Exception e) {
            return bytecode; // best-effort: return original if transform fails
        }
    }
}
