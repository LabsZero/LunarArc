package io.papermc.paper.configuration.serializer;

import io.leangen.geantyref.TypeToken;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.configurate.serialize.ScalarSerializer;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.util.EnumLookup;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import static io.leangen.geantyref.GenericTypeReflector.erase;

/**
 * Enum serializer that lists options if fails and accepts `-` as `_`.
 *
 * LunarArc note: matches real Paper's io.papermc.paper.configuration.serializer.EnumValueSerializer
 * exactly (verified directly against PaperMC/Paper-archive ver/1.21.1), with one deliberate change -
 * real Paper uses `LogUtils.getClassLogger()`, which calls a com.mojang.logging.LogUtils method
 * NeoForge 1.21.1's bundled com.mojang:logging (1.2.7) doesn't have, throwing NoSuchMethodError
 * the moment this class loaded. Plain SLF4J LoggerFactory.getLogger(...) is what
 * LogUtils.getClassLogger() resolves to internally anyway (it just adds caller-class detection
 * via StackWalker), so behavior is otherwise identical, including the important part that was
 * wrong in an earlier reimplementation done without this reference: an invalid enum value logs
 * an error and returns null (Configurate then falls back to the field's default) rather than
 * throwing - a plugin with a typo'd config value must not fail to load over it.
 */
public class EnumValueSerializer extends ScalarSerializer<Enum<?>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnumValueSerializer.class);

    public EnumValueSerializer() {
        super(new TypeToken<Enum<?>>() {});
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public @Nullable Enum<?> deserialize(final Type type, final Object obj) throws SerializationException {
        final String enumConstant = obj.toString();
        final Class<? extends Enum> typeClass = erase(type).asSubclass(Enum.class);
        @Nullable Enum<?> ret = EnumLookup.lookupEnum(typeClass, enumConstant);
        if (ret == null) {
            ret = EnumLookup.lookupEnum(typeClass, enumConstant.replace("-", "_"));
        }
        if (ret == null) {
            boolean longer = typeClass.getEnumConstants().length > 10;
            List<String> options = Arrays.stream(typeClass.getEnumConstants()).limit(10L).map(Enum::name).toList();
            LOGGER.error("Invalid enum constant provided, expected one of [" + String.join(", ", options) + (longer ? ", ..." : "") + "], but got " + enumConstant);
        }
        return ret;
    }

    @Override
    public Object serialize(final Enum<?> item, final Predicate<Class<?>> typeSupported) {
        return item.name();
    }
}
