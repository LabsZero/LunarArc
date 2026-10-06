package io.lunararcdevs.lunararc.common.server;

import io.papermc.paper.registry.PaperRegistryAccess;
import io.lunararcdevs.lunararc.api.Unsafe;
import io.papermc.paper.adventure.providers.ClickCallbackProviderImpl;
import io.papermc.paper.adventure.providers.DataComponentValueConverterProviderImpl;
import io.papermc.paper.adventure.providers.PlainTextComponentSerializerProviderImpl;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEventTypeProviderImpl;
import io.papermc.paper.registry.RegistryAccess;
import net.kyori.adventure.text.event.DataComponentValueConverterRegistry;
import net.kyori.adventure.text.serializer.gson.impl.GsonDataComponentValueConverterProvider;
import net.kyori.adventure.text.serializer.gson.impl.JSONComponentSerializerProviderImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;

public final class LunarArcPaperServiceBootstrap {
    private static final Logger LOGGER = LoggerFactory.getLogger("LunarArc");
    private static volatile boolean attempted;

    private LunarArcPaperServiceBootstrap() {}

    public static synchronized void ensureInstalled() {
        if (attempted) return;
        attempted = true;
        seedIfBroken(RegistryAccess::registryAccess,
                "io.papermc.paper.registry.RegistryAccessHolder", "INSTANCE",
                Optional.of(PaperRegistryAccess.INSTANCE));

        LifecycleEventTypeProviderImpl.ensureInstalled();
        installAdventureProviders();
        installPaperProviders();
    }

    private interface Installer {
        void install() throws Exception;
    }

    private static void installAdventureProviders() {
        install("Paper's click callback provider; ClickEvent.callback stays unsupported",
                LunarArcPaperServiceBootstrap::installClickCallbackProvider);
        install("Paper's plain text serializer; translatable components will render as raw keys",
                LunarArcPaperServiceBootstrap::installPlainTextProvider);
        install("adventure's JSON serializer provider; JSONComponentSerializer.json() may be unavailable",
                LunarArcPaperServiceBootstrap::installJsonSerializerProvider);
        install("Paper's data component converters; item hover components will not convert to JSON",
                LunarArcPaperServiceBootstrap::installDataComponentConverters);
    }

    private static void installPaperProviders() {
        install("Paper's brigadier argument types; ArgumentTypes will be unavailable",
                () -> seedOptional("io.papermc.paper.command.brigadier.argument.VanillaArgumentProvider", "PROVIDER",
                        io.papermc.paper.command.brigadier.argument.VanillaArgumentProviderImpl::new));
        install("Paper's brigadier message serializer; MessageComponentSerializer will be unavailable",
                () -> seedOptional("io.papermc.paper.command.brigadier.MessageComponentSerializerHolder", "PROVIDER",
                        io.papermc.paper.command.brigadier.MessageComponentSerializerImpl::new));
        install("Paper's feature flag provider; FeatureDependant#requiredFeatures will be unavailable",
                () -> seedOptional("io.papermc.paper.world.flag.FeatureFlagProvider", "PROVIDER",
                        io.papermc.paper.world.flag.PaperFeatureFlagProviderImpl::new));
        install("Paper's registry event types; RegistryEvents will be unavailable",
                () -> seedOptional("io.papermc.paper.registry.event.RegistryEventTypeProvider", "PROVIDER",
                        io.papermc.paper.registry.event.RegistryEventTypeProviderImpl::new));
        install("Adventure's boss bar provider; Audience#showBossBar will be unavailable",
                () -> seedOptional("net.kyori.adventure.bossbar.BossBarImpl$ImplementationAccessor", "SERVICE",
                        io.papermc.paper.adventure.providers.BossBarImplementationProvider::new));
    }

    private static void seedOptional(String holderClass, String name, java.util.function.Supplier<?> provider) throws Exception {
        Field field = staticField(holderClass, name);
        if (readStatic(field) instanceof Optional<?> present && present.isPresent()) return;
        writeStatic(field, Optional.of(provider.get()));
    }

    private static void install(String description, Installer installer) {
        try {
            installer.install();
        } catch (Throwable failure) {
            LOGGER.warn("Unable to install " + description, failure);
        }
    }

    private static Field staticField(String holderClass, String name) throws Exception {
        return Class.forName(holderClass).getDeclaredField(name);
    }

    private static Object readStatic(Field field) {
        return Unsafe.getObject(Unsafe.staticFieldBase(field), Unsafe.staticFieldOffset(field));
    }

    private static void writeStatic(Field field, Object value) {
        Unsafe.putObject(Unsafe.staticFieldBase(field), Unsafe.staticFieldOffset(field), value);
    }

    private static void installClickCallbackProvider() throws Exception {
        Field provider = staticField("net.kyori.adventure.text.event.ClickCallbackInternals", "PROVIDER");
        if (readStatic(provider) instanceof ClickCallbackProviderImpl) return;
        writeStatic(provider, new ClickCallbackProviderImpl());
    }

    private static void installPlainTextProvider() throws Exception {
        String impl = "net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializerImpl";
        Field service = staticField(impl, "SERVICE");
        if (readStatic(service) instanceof Optional<?> present && present.isPresent()) return;
        PlainTextComponentSerializerProviderImpl provider = new PlainTextComponentSerializerProviderImpl();
        writeStatic(service, Optional.of(provider));
        writeStatic(staticField(impl, "BUILDER"), provider.plainText());
        writeStatic(staticField(impl + "$Instances", "INSTANCE"), provider.plainTextSimple());
    }

    private static boolean gsonVisibleToAdventure() {
        try {
            ClassLoader adventureLoader = Class.forName("net.kyori.adventure.text.Component").getClassLoader();
            Class.forName("com.google.gson.JsonParseException", false, adventureLoader);
            return true;
        } catch (ClassNotFoundException | LinkageError unavailable) {
            LOGGER.debug("Gson is not visible to Adventure on this platform; skipping its JSON provider installs");
            return false;
        }
    }

    private static void installJsonSerializerProvider() throws Exception {
        if (!gsonVisibleToAdventure()) return;
        Field service = staticField("net.kyori.adventure.text.serializer.json.JSONComponentSerializerAccessor", "SERVICE");
        if (readStatic(service) instanceof Optional<?> present && present.isPresent()) return;
        writeStatic(service, Optional.of(new JSONComponentSerializerProviderImpl()));
    }

    @SuppressWarnings("unchecked")
    private static void installDataComponentConverters() throws Exception {
        if (!gsonVisibleToAdventure()) return;
        Field providers = staticField(DataComponentValueConverterRegistry.class.getName(), "PROVIDERS");
        Set<DataComponentValueConverterRegistry.Provider> current =
                (Set<DataComponentValueConverterRegistry.Provider>) readStatic(providers);
        Set<DataComponentValueConverterRegistry.Provider> merged = new LinkedHashSet<>(current);
        if (current.stream().noneMatch(GsonDataComponentValueConverterProvider.class::isInstance)) {
            merged.add(new GsonDataComponentValueConverterProvider());
        }
        if (current.stream().noneMatch(DataComponentValueConverterProviderImpl.class::isInstance)) {
            merged.add(new DataComponentValueConverterProviderImpl());
        }
        if (merged.size() != current.size()) writeStatic(providers, Collections.unmodifiableSet(merged));
    }

    /** Public so package-private donor types (like LifecycleEventTypeProvider) can call this from their own package. */
    public static void seedIfBroken(Callable<?> realAccessor, String holderClass, String field, Object value) {
        try {
            realAccessor.call();
            return;
        } catch (Throwable unresolved) {
            // Falls through to the direct seed below.
        }
        try {
            Class<?> holder = Class.forName(holderClass);
            Field instance = holder.getDeclaredField(field);
            Unsafe.putObject(Unsafe.staticFieldBase(instance), Unsafe.staticFieldOffset(instance), value);
        } catch (Throwable cannotSeed) {
            throw new IllegalStateException("Unable to install LunarArc's " + holderClass + " provider", cannotSeed);
        }
    }
}
