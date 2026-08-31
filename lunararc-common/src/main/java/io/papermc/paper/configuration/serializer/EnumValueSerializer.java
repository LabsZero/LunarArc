package io.papermc.paper.configuration.serializer;

import io.leangen.geantyref.GenericTypeReflector;
import org.spongepowered.configurate.serialize.ScalarSerializer;
import org.spongepowered.configurate.serialize.SerializationException;

import java.lang.reflect.Type;
import java.util.function.Predicate;

// LunarArc-owned: real Paper's donated bytecode version of this class calls
// com.mojang.logging.LogUtils.getClassLogger(), a method NeoForge 1.21.1's bundled
// com.mojang:logging (1.2.7, older than what Paper links against) doesn't have -
// NoSuchMethodError at <clinit> the moment any paper-plugin.yml-based plugin loaded.
// This reimplementation matches the same case-insensitive enum-by-name contract
// without touching that incompatible Mojang wrapper.
@SuppressWarnings({"unchecked", "rawtypes"})
public final class EnumValueSerializer extends ScalarSerializer<Enum> {

    public EnumValueSerializer() {
        super(Enum.class);
    }

    @Override
    public Enum deserialize(final Type type, final Object obj) throws SerializationException {
        final Class<?> rawType = GenericTypeReflector.erase(type);
        if (!rawType.isEnum()) {
            throw new SerializationException(type, "Type " + rawType + " is not an enum");
        }
        final String value = obj.toString();
        for (final Object constant : rawType.getEnumConstants()) {
            if (((Enum<?>) constant).name().equalsIgnoreCase(value)) {
                return (Enum<?>) constant;
            }
        }
        throw new SerializationException(type, "Invalid value `" + value + "` for enum type " + rawType.getSimpleName());
    }

    @Override
    protected Object serialize(final Enum item, final Predicate<Class<?>> typeSupported) {
        return item.name();
    }
}
