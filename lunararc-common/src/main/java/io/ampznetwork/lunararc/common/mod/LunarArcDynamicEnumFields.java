package io.ampznetwork.lunararc.common.mod;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Real {@link Field} objects for the enum constants LunarArc adds at runtime.
 *
 * <p>A normal enum constant is two things: an entry in the class's {@code $VALUES} array, and a
 * {@code public static final} field named after it. LunarArc adds modded items to
 * {@code org.bukkit.Material} by building the instances and extending that array, which is the only
 * option once the class is loaded - a class cannot gain fields afterwards. So the constants exist,
 * {@code values()} returns them, {@code valueOf} finds them, and {@code Material.class.getField(name)}
 * throws.</p>
 *
 * <p>That is not a hypothetical. Gson builds an adapter for an enum by walking
 * {@code getEnumConstants()} and calling {@code getField(constant.name())} on each one, to look for
 * a {@code @SerializedName}. On the first modded material it threw NoSuchFieldException, Gson turned
 * that into an AssertionError, and EssentialsX - which parses its item database with Gson - failed
 * to enable. Any library that looks up an enum constant by name reflectively hits the same wall.</p>
 *
 * <p>So the fields are made to exist. One holder class per enum is generated with a
 * {@code public static} field per dynamic constant, and each is set to the constant it names. The
 * Field handed back reads the right value, reports the right name and the right type, and carries
 * no annotations - which is accurate, because a runtime-built constant has none.</p>
 *
 * <p><strong>The one thing it cannot get right is {@code getDeclaringClass()}</strong>, which names
 * the holder rather than the enum. Nothing can fix that short of generating the enum class itself
 * with the modded constants compiled in, which needs the mod's item list before Bukkit's Material
 * is ever loaded. Callers that read the field - which is what a caller who looked one up wants -
 * get the correct answer; a caller inspecting where it was declared does not. That is a far smaller
 * inaccuracy than refusing to answer at all, and it is confined to constants that have no declared
 * field by construction.</p>
 *
 * <p>Every failure here is swallowed back to "no field", which is exactly the behaviour that
 * existed before this class. It can make reflection work; it must never be the reason it breaks.</p>
 */
public final class LunarArcDynamicEnumFields {

    private static final Map<Class<?>, Map<String, Field>> CACHE = new HashMap<>();
    private static final AtomicInteger COUNTER = new AtomicInteger();

    private LunarArcDynamicEnumFields() {
    }

    /** A field for a runtime-added constant of this enum, or null if there is no such constant. */
    public static Field find(Class<?> owner, String name) {
        if (owner == null || name == null || !owner.isEnum()) return null;
        Map<String, Field> fields;
        synchronized (CACHE) {
            fields = CACHE.get(owner);
            if (fields == null) {
                fields = build(owner);
                CACHE.put(owner, fields);
            }
        }
        return fields.get(name);
    }

    private static Map<String, Field> build(Class<?> owner) {
        try {
            Object[] constants = owner.getEnumConstants();
            if (constants == null || constants.length == 0) return Map.of();

            // Anything with a declared field is a constant the compiler produced, and getField
            // already answers for it. Only what is left needs a field inventing.
            java.util.Set<String> declared = new java.util.HashSet<>();
            for (Field field : owner.getDeclaredFields()) declared.add(field.getName());

            Map<String, Object> missing = new LinkedHashMap<>();
            for (Object constant : constants) {
                String name = ((Enum<?>) constant).name();
                if (!declared.contains(name) && isFieldName(name)) missing.put(name, constant);
            }
            if (missing.isEmpty()) return Map.of();

            Class<?> holder = defineHolder(owner, missing.keySet());
            Map<String, Field> result = new HashMap<>(missing.size() * 2);
            for (Map.Entry<String, Object> entry : missing.entrySet()) {
                Field field = holder.getField(entry.getKey());
                field.set(null, entry.getValue());
                result.put(entry.getKey(), field);
            }
            return result;
        } catch (Throwable failure) {
            // Back to "there is no such field", which is where this started.
            return Map.of();
        }
    }

    /**
     * Whether a constant's name can be a field name in a class file.
     *
     * <p>Checked per name rather than trusted, because one unusable name would otherwise fail the
     * whole class definition and take every other constant of that enum down with it. Enum names
     * built from a mod's resource location are fine; this is here so that a mod that manages
     * something stranger costs only its own entry.</p>
     */
    private static boolean isFieldName(String name) {
        if (name.isEmpty()) return false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (c == '.' || c == ';' || c == '[' || c == '/') return false;
        }
        return true;
    }

    private static Class<?> defineHolder(Class<?> owner, java.util.Collection<String> names) throws Throwable {
        // Defined in this class's own package so the lookup has the access to do it, and loaded by
        // this class's loader, which can see the enum type the fields are typed as.
        String simple = owner.getName().replace('.', '_').replace('$', '_');
        String internalName = Type.getInternalName(LunarArcDynamicEnumFields.class)
                + "_" + simple + "_" + COUNTER.incrementAndGet();
        String descriptor = Type.getDescriptor(owner);

        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER | Opcodes.ACC_FINAL,
                internalName, null, "java/lang/Object", null);
        for (String name : names) {
            // Not final: these are set by reflection once the class exists, and a final static
            // with no initializer in the class file is a field nothing may ever write.
            writer.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, name, descriptor, null, null)
                    .visitEnd();
        }
        writer.visitEnd();

        return MethodHandles.lookup().defineClass(writer.toByteArray());
    }
}
