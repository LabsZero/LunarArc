package io.ampznetwork.lunararc.common.mod;

import io.ampznetwork.lunararc.common.LunarArcPlatform;
import io.ampznetwork.lunararc.common.server.LunarArcVersionInfo;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Paper/Spigot -> Mojang runtime remapper.
 *
 * Paper 1.20.5+ runs Mojang-mapped Minecraft classes and remaps legacy
 * Spigot-mapped plugins at load time. LunarArc follows the same model. The
 * complete mapping table is generated at build time from the matching Mojang
 * server mappings and pinned Spigot BuildData revision, then loaded here.
 *
 * plugin-remap.tsv is deliberately only an override layer for hybrid-specific
 * edge cases. It is no longer the primary mapping database.
 */
public class LunarArcRemapper extends org.objectweb.asm.commons.Remapper {
    private static final String CRAFTBUKKIT_PREFIX = "org/bukkit/craftbukkit/";
    private static final String CRAFTBUKKIT_TARGET = CRAFTBUKKIT_PREFIX + LunarArcVersionInfo.craftBukkitPackage() + "/";
    private static final Map<String, Boolean> CRAFTBUKKIT_OVERRIDE_CACHE = new ConcurrentHashMap<>();

    /** Spigot internal class name -> Mojang internal class name. */
    private static final Map<String, String> CLASS_MAP = new HashMap<>();
    /** Mojang internal class name -> Spigot internal class name, used for descriptors. */
    private static final Map<String, String> MOJANG_TO_SPIGOT_CLASS = new HashMap<>();
    /** Keys use the Spigot owner/name/descriptor; values are Mojang member names. */
    private static final Map<MemberKey, String> FIELD_MAP = new HashMap<>();
    private static final Map<MemberKey, String> METHOD_MAP = new HashMap<>();
    private static final Map<MemberNameKey, String> FIELD_NAME_MAP = new HashMap<>();
    private static final Map<MemberNameKey, String> METHOD_NAME_MAP = new HashMap<>();
    private static final Map<String, String> RUNTIME_FIELD_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, String> RUNTIME_METHOD_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, String> BYTECODE_FIELD_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, String> BYTECODE_METHOD_CACHE = new ConcurrentHashMap<>();

    private final boolean remapNms;

    static {
        loadMappings();
    }

    public LunarArcRemapper() {
        this(true);
    }

    public LunarArcRemapper(boolean remapNms) {
        this.remapNms = remapNms;
    }

    private static void loadMappings() {
        String base = "mappings/" + LunarArcVersionInfo.minecraftVersion() + "/";
        ClassLoader loader = LunarArcRemapper.class.getClassLoader();

        try {
            // The Gradle build generates the complete mapping resource for this MC version.
            try (InputStream stream = loader.getResourceAsStream(base + "paper-reobf.tiny")) {
                if (stream == null) {
                    throw new IllegalStateException("Missing Paper reobf mappings " + base + "paper-reobf.tiny");
                }
                loadPaperMappings(stream);
            }

            // LunarArc-specific fixes can override Paper data, but should stay small.
            try (InputStream stream = loader.getResourceAsStream(base + "plugin-remap.tsv")) {
                if (stream != null) loadOverrides(stream);
            }
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static void loadPaperMappings(InputStream stream) throws Exception {
        List<String> lines;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            lines = reader.lines().toList();
        }
        if (lines.isEmpty()) throw new IllegalStateException("Paper mapping resource is empty");

        String[] header = lines.get(0).split("\\t", -1);
        if (header.length == 0) throw new IllegalStateException("Invalid Paper mapping header");

        List<PendingMember> pending = new ArrayList<>();
        if ("tiny".equals(header[0])) {
            parseTinyV2(lines, header, pending);
        } else if ("v1".equals(header[0])) {
            parseTinyV1(lines, header, pending);
        } else if ("tsrg2".equals(header[0])) {
            parseTsrg2(lines, pending);
        } else {
            throw new IllegalStateException("Unsupported Paper mapping format: " + lines.get(0));
        }

        // Tiny member descriptors are expressed in the first/source namespace
        // (Mojang for Paper's reobf mapping). Convert descriptors to Spigot so
        // ASM lookups from Spigot-mapped plugin bytecode match exactly.
        for (PendingMember member : pending) {
            String spigotDescriptor = mapDescriptorClasses(member.mojangDescriptor, MOJANG_TO_SPIGOT_CLASS);
            MemberKey key = new MemberKey(member.spigotOwner, member.spigotName, spigotDescriptor);
            if (member.method) {
                METHOD_MAP.put(key, member.mojangName);
                addUniqueNameMapping(METHOD_NAME_MAP, new MemberNameKey(member.spigotOwner, member.spigotName), member.mojangName);
            } else {
                FIELD_MAP.put(key, member.mojangName);
                addUniqueNameMapping(FIELD_NAME_MAP, new MemberNameKey(member.spigotOwner, member.spigotName), member.mojangName);
            }
        }
    }

    private static void parseTinyV2(List<String> lines, String[] header, List<PendingMember> pending) {
        if (header.length < 5 || !"2".equals(header[1])) {
            throw new IllegalStateException("Unsupported Tiny header: " + lines.get(0));
        }
        int sourceIndex = 3;
        int targetIndex = findSpigotNamespace(header, sourceIndex + 1);
        if (targetIndex < 0) targetIndex = header.length - 1;

        String mojangOwner = null;
        String spigotOwner = null;
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isEmpty() || line.charAt(0) == '#') continue;
            String[] p = line.split("\\t", -1);

            if (p.length >= 3 && "c".equals(p[0])) {
                mojangOwner = p[1];
                int mappedColumn = targetIndex - sourceIndex + 1;
                if (mappedColumn >= p.length) continue;
                spigotOwner = p[mappedColumn];
                if (!mojangOwner.isEmpty() && !spigotOwner.isEmpty()) {
                    CLASS_MAP.put(spigotOwner, mojangOwner);
                    MOJANG_TO_SPIGOT_CLASS.put(mojangOwner, spigotOwner);
                }
                continue;
            }

            // Tiny v2 nested members: <tab>f/m <desc> <srcName> <mappedName>...
            if (p.length >= 5 && p[0].isEmpty() && ("f".equals(p[1]) || "m".equals(p[1]))
                    && mojangOwner != null && spigotOwner != null) {
                int mappedColumn = targetIndex - sourceIndex + 3;
                if (mappedColumn >= p.length) continue;
                String descriptor = p[2];
                String mojangName = p[3];
                String spigotName = p[mappedColumn];
                if (!mojangName.isEmpty() && !spigotName.isEmpty()) {
                    pending.add(new PendingMember("m".equals(p[1]), spigotOwner, spigotName, descriptor, mojangName));
                }
            }
        }
    }

    private static void parseTinyV1(List<String> lines, String[] header, List<PendingMember> pending) {
        int targetIndex = findSpigotNamespace(header, 1);
        if (targetIndex < 0) targetIndex = header.length - 1;
        int mappedNameOffset = targetIndex; // names begin at column 1 for CLASS and 3 for members

        for (int i = 1; i < lines.size(); i++) {
            String[] p = lines.get(i).split("\\t", -1);
            if (p.length < 3) continue;
            switch (p[0]) {
                case "CLASS" -> {
                    if (mappedNameOffset >= p.length) continue;
                    String mojang = p[1];
                    String spigot = p[mappedNameOffset];
                    if (!mojang.isEmpty() && !spigot.isEmpty()) {
                        CLASS_MAP.put(spigot, mojang);
                        MOJANG_TO_SPIGOT_CLASS.put(mojang, spigot);
                    }
                }
                case "FIELD", "METHOD" -> {
                    // Tiny v1: TYPE owner descriptor sourceName mappedName...
                    int mappedColumn = targetIndex + 2;
                    if (p.length <= mappedColumn) continue;
                    String mojangOwner = p[1];
                    String spigotOwner = MOJANG_TO_SPIGOT_CLASS.getOrDefault(mojangOwner, mojangOwner);
                    pending.add(new PendingMember("METHOD".equals(p[0]), spigotOwner, p[mappedColumn], p[2], p[3]));
                }
                default -> {
                }
            }
        }
    }


    private static void parseTsrg2(List<String> lines, List<PendingMember> pending) {
        String mojangOwner = null;
        String spigotOwner = null;
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank() || line.startsWith("#")) continue;
            if (!Character.isWhitespace(line.charAt(0))) {
                String[] p = line.trim().split("\\s+");
                if (p.length >= 2) {
                    mojangOwner = p[0];
                    spigotOwner = p[1];
                    CLASS_MAP.put(spigotOwner, mojangOwner);
                    MOJANG_TO_SPIGOT_CLASS.put(mojangOwner, spigotOwner);
                }
                continue;
            }
            if (mojangOwner == null || spigotOwner == null) continue;
            String[] p = line.trim().split("\\s+");
            if (p.length == 2) {
                // Field without descriptor in TSRG2.
                pending.add(new PendingMember(false, spigotOwner, p[1], "*", p[0]));
            } else if (p.length >= 3 && p[1].startsWith("(")) {
                pending.add(new PendingMember(true, spigotOwner, p[2], p[1], p[0]));
            } else if (p.length >= 3) {
                pending.add(new PendingMember(false, spigotOwner, p[p.length - 1], p[1], p[0]));
            }
        }
    }

    private static int findSpigotNamespace(String[] header, int start) {
        for (int i = start; i < header.length; i++) {
            String value = header[i].toLowerCase(java.util.Locale.ROOT);
            if (value.contains("spigot") || value.contains("reobf")) return i;
        }
        return -1;
    }

    private static void loadOverrides(InputStream stream) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split("\\t", -1);
                switch (parts[0]) {
                    case "CLASS" -> {
                        if (parts.length != 3) throw invalid(line);
                        CLASS_MAP.put(parts[1], parts[2]);
                        MOJANG_TO_SPIGOT_CLASS.put(parts[2], parts[1]);
                    }
                    case "FIELD" -> {
                        if (parts.length != 5) throw invalid(line);
                        FIELD_MAP.put(new MemberKey(parts[1], parts[2], parts[3]), parts[4]);
                        addUniqueNameMapping(FIELD_NAME_MAP, new MemberNameKey(parts[1], parts[2]), parts[4]);
                    }
                    case "METHOD" -> {
                        if (parts.length != 5) throw invalid(line);
                        METHOD_MAP.put(new MemberKey(parts[1], parts[2], parts[3]), parts[4]);
                        addUniqueNameMapping(METHOD_NAME_MAP, new MemberNameKey(parts[1], parts[2]), parts[4]);
                    }
                    default -> throw invalid(line);
                }
            }
        }
    }

    private static final String AMBIGUOUS = "\u0000";

    private static void addUniqueNameMapping(Map<MemberNameKey, String> map, MemberNameKey key, String value) {
        String previous = map.putIfAbsent(key, value);
        if (previous != null && !previous.equals(value)) map.put(key, AMBIGUOUS);
    }

    private static IllegalStateException invalid(String line) {
        return new IllegalStateException("Invalid plugin mapping entry: " + line);
    }

    private static boolean hasCraftBukkitOverride(String internalName) {
        String topLevel = internalName;
        int nested = topLevel.indexOf('$');
        if (nested > 0) topLevel = topLevel.substring(0, nested);
        final String classResource = topLevel + ".class";
        return CRAFTBUKKIT_OVERRIDE_CACHE.computeIfAbsent(topLevel, ignored -> {
            ClassLoader loader = LunarArcRemapper.class.getClassLoader();
            return loader.getResource(classResource) != null;
        });
    }

    @Override
    public String map(String internalName) {
        if (internalName == null) return null;

        if (internalName.startsWith(CRAFTBUKKIT_PREFIX)) {
            String suffix = internalName.substring(CRAFTBUKKIT_PREFIX.length());
            if (suffix.startsWith("v1_")) {
                int slash = suffix.indexOf('/');
                if (slash >= 0) suffix = suffix.substring(slash + 1);
            }

            // Paper 1.20.5+ ships CraftBukkit unversioned. LunarArc keeps a small
            // versioned override layer for hybrid-aware implementations, but it must
            // not force every Paper class into that package or plugins will be
            // remapped to classes that do not exist. Prefer the LunarArc override
            // only when its class file is actually present; otherwise retain the
            // complete unversioned Paper implementation surface.
            String versioned = CRAFTBUKKIT_TARGET + suffix;
            if (hasCraftBukkitOverride(versioned)) return versioned;
            return CRAFTBUKKIT_PREFIX + suffix;
        }

        if (!remapNms) return internalName;
        String mapped = CLASS_MAP.get(internalName);
        if (mapped != null) return mapped;

        int nested = internalName.indexOf('$');
        if (nested > 0) {
            String mappedOwner = CLASS_MAP.get(internalName.substring(0, nested));
            if (mappedOwner != null) return mappedOwner + internalName.substring(nested);
        }
        return internalName;
    }

    @Override
    public String mapFieldName(String owner, String name, String descriptor) {
        if (!remapNms || owner == null || name == null) return name;
        String spigotOwner = toSpigotOwner(owner);
        String lookupDescriptor = toSpigotDescriptor(descriptor);
        String mapped = FIELD_MAP.get(new MemberKey(spigotOwner, name, lookupDescriptor));
        if (mapped == null && !java.util.Objects.equals(lookupDescriptor, descriptor)) {
            mapped = FIELD_MAP.get(new MemberKey(spigotOwner, name, descriptor));
        }
        if (mapped == null) mapped = FIELD_MAP.get(new MemberKey(spigotOwner, name, "*"));
        if (mapped == null) {
            String unique = FIELD_NAME_MAP.get(new MemberNameKey(spigotOwner, name));
            if (unique != null && !AMBIGUOUS.equals(unique)) mapped = unique;
        }
        if (mapped == null && spigotOwner.startsWith("net/minecraft/")) {
            String key = spigotOwner + '#' + name + '#' + descriptor;
            mapped = BYTECODE_FIELD_CACHE.computeIfAbsent(key,
                    ignored -> resolveInheritedBytecodeMember(spigotOwner, name, descriptor, false));
            if (name.equals(mapped)) mapped = null;
        }
        return mapped != null ? mapped : name;
    }

    @Override
    public String mapMethodName(String owner, String name, String descriptor) {
        if (!remapNms || "<init>".equals(name) || "<clinit>".equals(name)) return name;
        String spigotOwner = toSpigotOwner(owner);
        String lookupDescriptor = toSpigotDescriptor(descriptor);
        String mapped = METHOD_MAP.get(new MemberKey(spigotOwner, name, lookupDescriptor));
        if (mapped == null && !java.util.Objects.equals(lookupDescriptor, descriptor)) {
            mapped = METHOD_MAP.get(new MemberKey(spigotOwner, name, descriptor));
        }
        if (mapped == null) mapped = METHOD_MAP.get(new MemberKey(spigotOwner, name, "*"));
        if (mapped == null) {
            String unique = METHOD_NAME_MAP.get(new MemberNameKey(spigotOwner, name));
            if (unique != null && !AMBIGUOUS.equals(unique)) mapped = unique;
        }
        if (mapped == null && spigotOwner.startsWith("net/minecraft/")) {
            String key = spigotOwner + '#' + name + '#' + descriptor;
            mapped = BYTECODE_METHOD_CACHE.computeIfAbsent(key,
                    ignored -> resolveInheritedBytecodeMember(spigotOwner, name, descriptor, true));
            if (name.equals(mapped)) mapped = null;
        }
        return mapped != null ? mapped : name;
    }

    private String resolveInheritedBytecodeMember(String spigotOwner, String name, String descriptor, boolean method) {
        String mojangOwner = CLASS_MAP.get(spigotOwner);
        if (mojangOwner == null) return name;
        try {
            ClassLoader loader = LunarArcPlatform.getModClassLoader();
            if (loader == null) loader = LunarArcRemapper.class.getClassLoader();
            Class<?> type = Class.forName(mojangOwner.replace('/', '.'), false, loader);
            for (Class<?> current = type; current != null; current = current.getSuperclass()) {
                String currentSpigot = MOJANG_TO_SPIGOT_CLASS.getOrDefault(
                        current.getName().replace('.', '/'), current.getName().replace('.', '/'));
                Map<MemberKey, String> mappings = method ? METHOD_MAP : FIELD_MAP;
                String lookupDescriptor = toSpigotDescriptor(descriptor);
                String mapped = mappings.get(new MemberKey(currentSpigot, name, lookupDescriptor));
                if (mapped == null) mapped = mappings.get(new MemberKey(currentSpigot, name, descriptor));
                if (mapped == null) mapped = mappings.get(new MemberKey(currentSpigot, name, "*"));
                if (mapped == null) {
                    Map<MemberNameKey, String> names = method ? METHOD_NAME_MAP : FIELD_NAME_MAP;
                    String unique = names.get(new MemberNameKey(currentSpigot, name));
                    if (unique != null && !AMBIGUOUS.equals(unique)) mapped = unique;
                }
                if (mapped != null) return mapped;
                if (method) {
                    String fromInterface = resolveInterfaceBytecodeMember(current.getInterfaces(), name, descriptor);
                    if (fromInterface != null) return fromInterface;
                }
            }
        } catch (ClassNotFoundException | LinkageError ignored) {
        }
        return name;
    }

    private String resolveInterfaceBytecodeMember(Class<?>[] interfaces, String name, String descriptor) {
        for (Class<?> iface : interfaces) {
            String spigot = MOJANG_TO_SPIGOT_CLASS.getOrDefault(
                    iface.getName().replace('.', '/'), iface.getName().replace('.', '/'));
            String lookupDescriptor = toSpigotDescriptor(descriptor);
            String mapped = METHOD_MAP.get(new MemberKey(spigot, name, lookupDescriptor));
            if (mapped == null) mapped = METHOD_MAP.get(new MemberKey(spigot, name, descriptor));
            if (mapped == null) mapped = METHOD_MAP.get(new MemberKey(spigot, name, "*"));
            if (mapped == null) {
                String unique = METHOD_NAME_MAP.get(new MemberNameKey(spigot, name));
                if (unique != null && !AMBIGUOUS.equals(unique)) mapped = unique;
            }
            if (mapped != null) return mapped;
            mapped = resolveInterfaceBytecodeMember(iface.getInterfaces(), name, descriptor);
            if (mapped != null) return mapped;
        }
        return null;
    }

    /** Resolve a reflective field name, including inherited declarations. */
    public String mapRuntimeFieldName(Class<?> runtimeOwner, String spigotName) {
        if (!remapNms || runtimeOwner == null || spigotName == null) return spigotName;
        String cacheKey = runtimeOwner.getName() + '#' + spigotName;
        return RUNTIME_FIELD_CACHE.computeIfAbsent(cacheKey,
                ignored -> resolveRuntimeMember(runtimeOwner, spigotName, false));
    }

    /** Resolve a reflective method name, including inherited/interface declarations. */
    public String mapRuntimeMethodName(Class<?> runtimeOwner, String spigotName) {
        if (!remapNms || runtimeOwner == null || spigotName == null) return spigotName;
        String cacheKey = runtimeOwner.getName() + '#' + spigotName;
        return RUNTIME_METHOD_CACHE.computeIfAbsent(cacheKey,
                ignored -> resolveRuntimeMember(runtimeOwner, spigotName, true));
    }

    private String resolveRuntimeMember(Class<?> runtimeOwner, String spigotName, boolean method) {
        for (Class<?> current = runtimeOwner; current != null; current = current.getSuperclass()) {
            String spigotOwner = MOJANG_TO_SPIGOT_CLASS.getOrDefault(current.getName().replace('.', '/'),
                    current.getName().replace('.', '/'));
            Map<MemberNameKey, String> names = method ? METHOD_NAME_MAP : FIELD_NAME_MAP;
            String unique = names.get(new MemberNameKey(spigotOwner, spigotName));
            if (unique != null && !AMBIGUOUS.equals(unique)) return unique;
            if (method) {
                for (Class<?> iface : current.getInterfaces()) {
                    String resolved = resolveRuntimeMember(iface, spigotName, true);
                    if (!resolved.equals(spigotName)) return resolved;
                }
            }
        }
        return spigotName;
    }

    private String toSpigotOwner(String owner) {
        String normalized = owner;
        if (CLASS_MAP.containsKey(normalized)) return normalized;
        return MOJANG_TO_SPIGOT_CLASS.getOrDefault(normalized, normalized);
    }

    private static String toSpigotDescriptor(String descriptor) {
        return mapDescriptorClasses(descriptor, MOJANG_TO_SPIGOT_CLASS);
    }

    public byte[] transform(byte[] bytecode, String className) {
        return transformInternal(bytecode, className == null ? "<unknown>" : className);
    }

    public byte[] transform(byte[] bytecode) {
        return transformInternal(bytecode, "<unknown>");
    }

    private byte[] transformInternal(byte[] bytecode, String className) {
        if (bytecode == null || bytecode.length < 8) return bytecode;
        try {
            ClassReader reader = new ClassReader(bytecode);
            ClassWriter writer = new ClassWriter(0);
            ClassVisitor remapper = new ClassRemapper(writer, this);
            ClassVisitor visitor = remapNms ? new ReflectionMemberVisitor(remapper) : remapper;
            visitor = compatibilityVisitor(visitor, className);
            reader.accept(visitor, 0);
            return writer.toByteArray();
        } catch (RuntimeException e) {
            throw new IllegalStateException("Failed to remap plugin class " + className, e);
        }
    }

    private ClassVisitor compatibilityVisitor(ClassVisitor delegate, String className) {
        // DecentHolograms 2.10.1 uses Server#getVersion() only when it detects
        // Paper, then splits the normal Paper value (git-Paper-133 ...) at '-'.
        // That yields "git" instead of the Minecraft version and self-disables.
        // Its v1_21_R1 support is otherwise present. Rewrite only that detector
        // class to use Bukkit's stable API-version value, exactly as its Spigot
        // branch already does. Do not alter Server#getVersion() globally.
        if (!"eu/decentsoftware/holograms/api/utils/reflect/Version".equals(className)) {
            return delegate;
        }
        return new ClassVisitor(Opcodes.ASM9, delegate) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                MethodVisitor method = super.visitMethod(access, name, descriptor, signature, exceptions);
                return new MethodVisitor(Opcodes.ASM9, method) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String methodName, String methodDescriptor, boolean isInterface) {
                        if (("org/bukkit/Server".equals(owner) || owner.startsWith("org/bukkit/craftbukkit/"))
                                && "getVersion".equals(methodName)
                                && "()Ljava/lang/String;".equals(methodDescriptor)) {
                            super.visitMethodInsn(opcode, owner, "getBukkitVersion", methodDescriptor, isInterface);
                            return;
                        }
                        super.visitMethodInsn(opcode, owner, methodName, methodDescriptor, isInterface);
                    }
                };
            }
        };
    }

    private String mapClassNameString(String value) {
        if (value == null || value.isEmpty()) return value;
        boolean binaryName = value.indexOf('.') >= 0 && value.indexOf('/') < 0;
        String internal = binaryName ? value.replace('.', '/') : value;
        if (!internal.startsWith(CRAFTBUKKIT_PREFIX) && !internal.startsWith("net/minecraft/")) return value;
        String mapped = map(internal);
        return mapped.equals(internal) ? value : (binaryName ? mapped.replace('/', '.') : mapped);
    }

    private final class ReflectionMemberVisitor extends ClassVisitor {
        private ReflectionMemberVisitor(ClassVisitor delegate) {
            super(Opcodes.ASM9, delegate);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            MethodVisitor delegate = super.visitMethod(access, name, descriptor, signature, exceptions);
            return new MethodVisitor(Opcodes.ASM9, delegate) {
                private String recentClassOwner;

                @Override
                public void visitLdcInsn(Object value) {
                    if (value instanceof Type type && type.getSort() == Type.OBJECT) {
                        recentClassOwner = type.getInternalName();
                        super.visitLdcInsn(value);
                        return;
                    }
                    if (value instanceof String text) {
                        String rewritten = mapClassNameString(text);
                        if (recentClassOwner != null) {
                            String field = mapFieldName(recentClassOwner, text, "*");
                            if (!field.equals(text)) rewritten = field;
                            else {
                                String method = mapMethodName(recentClassOwner, text, "*");
                                if (!method.equals(text)) rewritten = method;
                            }
                        }
                        super.visitLdcInsn(rewritten);
                        recentClassOwner = null;
                        return;
                    }
                    recentClassOwner = null;
                    super.visitLdcInsn(value);
                }

                @Override
                public void visitMethodInsn(int opcode, String owner, String methodName, String methodDescriptor, boolean isInterface) {
                    if ("java/lang/Class".equals(owner)) {
                        String bridgeOwner = "io/ampznetwork/lunararc/common/mod/LunarArcReflectionBridge";
                        switch (methodName) {
                            case "getField" -> {
                                recentClassOwner = null;
                                super.visitMethodInsn(Opcodes.INVOKESTATIC, bridgeOwner, "getField",
                                        "(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/reflect/Field;", false);
                                return;
                            }
                            case "getDeclaredField" -> {
                                recentClassOwner = null;
                                super.visitMethodInsn(Opcodes.INVOKESTATIC, bridgeOwner, "getDeclaredField",
                                        "(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/reflect/Field;", false);
                                return;
                            }
                            case "getMethod" -> {
                                recentClassOwner = null;
                                super.visitMethodInsn(Opcodes.INVOKESTATIC, bridgeOwner, "getMethod",
                                        "(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;", false);
                                return;
                            }
                            case "getDeclaredMethod" -> {
                                recentClassOwner = null;
                                super.visitMethodInsn(Opcodes.INVOKESTATIC, bridgeOwner, "getDeclaredMethod",
                                        "(Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;", false);
                                return;
                            }
                            default -> {
                            }
                        }
                    }
                    if (!("java/lang/Class".equals(owner) && "forName".equals(methodName))) recentClassOwner = null;
                    super.visitMethodInsn(opcode, owner, methodName, methodDescriptor, isInterface);
                }

                @Override
                public void visitVarInsn(int opcode, int varIndex) {
                    recentClassOwner = null;
                    super.visitVarInsn(opcode, varIndex);
                }
            };
        }
    }

    private static String mapDescriptorClasses(String descriptor, Map<String, String> classes) {
        if (descriptor == null || descriptor.isEmpty() || "*".equals(descriptor)) return descriptor;
        StringBuilder out = new StringBuilder(descriptor.length());
        for (int i = 0; i < descriptor.length(); i++) {
            char c = descriptor.charAt(i);
            out.append(c);
            if (c == 'L') {
                int end = descriptor.indexOf(';', i);
                if (end < 0) break;
                String name = descriptor.substring(i + 1, end);
                out.append(classes.getOrDefault(name, name));
                out.append(';');
                i = end;
            }
        }
        return out.toString();
    }

    private record MemberKey(String owner, String name, String descriptor) {
    }

    private record MemberNameKey(String owner, String name) {
    }

    private record PendingMember(boolean method, String spigotOwner, String spigotName,
                                 String mojangDescriptor, String mojangName) {
    }
}
