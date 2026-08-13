package io.ampznetwork.lunararc.common.server;

import io.ampznetwork.lunararc.common.LunarArcPaths;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.PluginClassLoader;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Runtime support for the modern paper-plugin.yml loader/bootstrapper model.
 *
 * The implementation deliberately talks to the Paper API by reflection. This
 * keeps LunarArc isolated from Paper server implementation classes while still
 * honoring the public PluginLoader, ClassPathLibrary and PluginBootstrap
 * contracts exposed to plugins.
 */
public final class LunarArcPaperPluginSupport {
    private LunarArcPaperPluginSupport() {}

    public static java.util.Set<String> noJoinClasspathDependencies(File pluginFile) {
        Map<String, Object> config = readPaperConfig(pluginFile);
        Object dependenciesValue = config.get("dependencies");
        if (!(dependenciesValue instanceof Map<?, ?> dependencies)) return java.util.Set.of();
        Object serverValue = dependencies.get("server");
        if (!(serverValue instanceof Map<?, ?> serverDependencies)) return java.util.Set.of();
        java.util.LinkedHashSet<String> result = new java.util.LinkedHashSet<>();
        for (Map.Entry<?, ?> entry : serverDependencies.entrySet()) {
            if (!(entry.getValue() instanceof Map<?, ?> dependency)) continue;
            Object join = dependency.get("join-classpath");
            if (join != null && !Boolean.parseBoolean(String.valueOf(join))) {
                result.add(normalizePluginName(String.valueOf(entry.getKey())));
            }
        }
        return java.util.Collections.unmodifiableSet(result);
    }

    public static ClassLoader createLibraryLoader(File pluginFile, PluginDescriptionFile description,
                                                   File dataFolder, ClassLoader parent) {
        Map<String, Object> config = readPaperConfig(pluginFile);
        String loaderName = string(config.get("loader"));
        if (loaderName == null) return null;

        URLClassLoader loaderClassLoader = null;
        try {
            loaderClassLoader = new URLClassLoader(new URL[]{pluginFile.toURI().toURL()}, parent);
            Class<?> pluginLoaderInterface = Class.forName(
                    "io.papermc.paper.plugin.loader.PluginLoader", true, parent);
            Class<?> builderInterface = Class.forName(
                    "io.papermc.paper.plugin.loader.PluginClasspathBuilder", true, parent);
            Class<?> libraryStoreInterface = Class.forName(
                    "io.papermc.paper.plugin.loader.library.LibraryStore", true, parent);

            Class<?> loaderClass = Class.forName(loaderName, true, loaderClassLoader);
            Object loader = loaderClass.getDeclaredConstructor().newInstance();
            if (!pluginLoaderInterface.isInstance(loader)) {
                throw new IllegalStateException(loaderName + " does not implement Paper PluginLoader");
            }

            List<Object> classPathLibraries = new ArrayList<>();
            Object context = createProviderContext(parent, description, dataFolder, pluginFile);
            Object builder = Proxy.newProxyInstance(parent, new Class<?>[]{builderInterface}, (proxy, method, args) -> {
                return switch (method.getName()) {
                    case "addLibrary" -> {
                        if (args != null && args.length == 1 && args[0] != null) classPathLibraries.add(args[0]);
                        yield proxy;
                    }
                    case "getContext" -> context;
                    case "toString" -> "LunarArcPluginClasspathBuilder[" + description.getName() + "]";
                    default -> primitiveDefault(method.getReturnType());
                };
            });

            Method classloader = pluginLoaderInterface.getMethod("classloader", builderInterface);
            classloader.invoke(loader, builder);

            List<Path> resolved = new ArrayList<>();
            Object libraryStore = Proxy.newProxyInstance(parent, new Class<?>[]{libraryStoreInterface}, (proxy, method, args) -> {
                if ("addLibrary".equals(method.getName()) && args != null && args.length == 1 && args[0] instanceof Path path) {
                    resolved.add(path);
                    return null;
                }
                return primitiveDefault(method.getReturnType());
            });

            for (Object library : classPathLibraries) {
                Method register = library.getClass().getMethod("register", libraryStoreInterface);
                register.invoke(library, libraryStore);
            }

            if (resolved.isEmpty()) {
                loaderClassLoader.close();
                return null;
            }

            Path pluginLibraryRoot = LunarArcPaths.pluginLibraries().resolve(safeName(description.getName()));
            Files.createDirectories(pluginLibraryRoot);
            LinkedHashMap<Path, URL> urls = new LinkedHashMap<>();
            for (Path source : resolved) {
                if (source == null || !Files.isRegularFile(source)) continue;
                Path cached = cacheLibrary(source, pluginLibraryRoot);
                urls.putIfAbsent(cached, cached.toUri().toURL());
            }

            loaderClassLoader.close();
            if (urls.isEmpty()) return null;
            return new URLClassLoader(urls.values().toArray(URL[]::new), parent);
        } catch (Throwable error) {
            if (loaderClassLoader != null) {
                try { loaderClassLoader.close(); } catch (IOException ignored) {}
            }
            throw new IllegalStateException("Could not prepare Paper PluginLoader for "
                    + description.getName() + ": " + rootMessage(error), unwrap(error));
        }
    }

    public static JavaPlugin createBootstrappedPlugin(PluginClassLoader pluginLoader, File pluginFile,
                                                        PluginDescriptionFile description, File dataFolder) {
        Map<String, Object> config = readPaperConfig(pluginFile);
        String bootstrapperName = string(config.get("bootstrapper"));
        if (bootstrapperName == null) return null;

        try {
            ClassLoader apiLoader = LunarArcPaperPluginSupport.class.getClassLoader();
            Class<?> bootstrapInterface = Class.forName(
                    "io.papermc.paper.plugin.bootstrap.PluginBootstrap", true, apiLoader);
            Class<?> bootstrapContextInterface = Class.forName(
                    "io.papermc.paper.plugin.bootstrap.BootstrapContext", true, apiLoader);
            Class<?> providerContextInterface = Class.forName(
                    "io.papermc.paper.plugin.bootstrap.PluginProviderContext", true, apiLoader);

            Class<?> bootstrapClass = pluginLoader.findClass(bootstrapperName, false);
            Object bootstrapper = bootstrapClass.getDeclaredConstructor().newInstance();
            if (!bootstrapInterface.isInstance(bootstrapper)) {
                throw new IllegalStateException(bootstrapperName + " does not implement Paper PluginBootstrap");
            }

            Object context = createBootstrapContext(apiLoader, bootstrapContextInterface,
                    description, dataFolder, pluginFile);
            bootstrapInterface.getMethod("bootstrap", bootstrapContextInterface).invoke(bootstrapper, context);

            Method createPlugin = bootstrapInterface.getMethod("createPlugin", providerContextInterface);
            Object plugin = createPlugin.invoke(bootstrapper, context);
            if (!(plugin instanceof JavaPlugin javaPlugin)) {
                throw new IllegalStateException("Paper bootstrapper did not create a JavaPlugin instance");
            }
            return javaPlugin;
        } catch (Throwable error) {
            throw new IllegalStateException("Could not bootstrap Paper plugin " + description.getName()
                    + ": " + rootMessage(error), unwrap(error));
        }
    }

    private static Object createBootstrapContext(ClassLoader loader, Class<?> bootstrapContextInterface,
                                                 PluginDescriptionFile description, File dataFolder, File pluginFile)
            throws ClassNotFoundException {
        Object provider = createProviderContext(loader, description, dataFolder, pluginFile);
        Object lifecycle = LunarArcLifecycleEventManager.create();
        return Proxy.newProxyInstance(loader, new Class<?>[]{bootstrapContextInterface}, (proxy, method, args) -> {
            if ("getLifecycleManager".equals(method.getName())) return lifecycle;
            if ("getPluginMeta".equals(method.getName())) return new LunarArcPluginMeta(description);
            try {
                Method providerMethod = provider.getClass().getMethod(method.getName(), method.getParameterTypes());
                return providerMethod.invoke(provider, args);
            } catch (ReflectiveOperationException ignored) {
                return providerContextValue(method.getName(), description, dataFolder, pluginFile);
            }
        });
    }

    private static Object createProviderContext(ClassLoader loader, PluginDescriptionFile description,
                                                File dataFolder, File pluginFile) throws ClassNotFoundException {
        Class<?> providerContextInterface = Class.forName(
                "io.papermc.paper.plugin.bootstrap.PluginProviderContext", true, loader);
        return Proxy.newProxyInstance(loader, new Class<?>[]{providerContextInterface}, (proxy, method, args) ->
                providerContextValue(method.getName(), description, dataFolder, pluginFile));
    }

    private static Object providerContextValue(String method, PluginDescriptionFile description,
                                               File dataFolder, File pluginFile) {
        return switch (method) {
            case "getConfiguration", "getPluginMeta" -> new LunarArcPluginMeta(description);
            case "getDataDirectory" -> dataFolder.toPath();
            case "getPluginSource" -> pluginFile.toPath();
            case "getLogger" -> net.kyori.adventure.text.logger.slf4j.ComponentLogger.logger(description.getName());
            case "toString" -> "LunarArcPluginProviderContext[" + description.getName() + "]";
            default -> null;
        };
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> readPaperConfig(File pluginFile) {
        try (JarFile jar = new JarFile(pluginFile)) {
            JarEntry entry = jar.getJarEntry("paper-plugin.yml");
            if (entry == null) return Map.of();
            try (InputStream stream = jar.getInputStream(entry)) {
                Object value = new Yaml().load(stream);
                return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
            }
        } catch (IOException error) {
            throw new IllegalStateException("Could not read paper-plugin.yml from " + pluginFile.getName(), error);
        }
    }

    private static Path cacheLibrary(Path source, Path root) throws IOException {
        String fileName = source.getFileName() == null ? "library.jar" : source.getFileName().toString();
        String prefix = sha256(source).substring(0, 16);
        Path destination = root.resolve(prefix + "-" + fileName);
        if (Files.isRegularFile(destination) && Files.size(destination) == Files.size(source)) return destination;
        Path temp = destination.resolveSibling(destination.getFileName() + ".tmp");
        Files.copy(source, temp, StandardCopyOption.REPLACE_EXISTING);
        try {
            Files.move(temp, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
            Files.move(temp, destination, StandardCopyOption.REPLACE_EXISTING);
        }
        return destination;
    }

    private static String sha256(Path path) throws IOException {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            try (InputStream in = Files.newInputStream(path)) {
                byte[] buffer = new byte[128 * 1024];
                int read;
                while ((read = in.read(buffer)) >= 0) if (read > 0) digest.update(buffer, 0, read);
            }
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static Object primitiveDefault(Class<?> type) {
        if (type == void.class || !type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0F;
        if (type == double.class) return 0D;
        return null;
    }

    private static String normalizePluginName(String value) {
        return value.replace(' ', '_').toLowerCase(java.util.Locale.ROOT);
    }

    private static String safeName(String value) {
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static String string(Object value) {
        if (value == null) return null;
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private static Throwable unwrap(Throwable error) {
        Throwable current = error;
        while ((current instanceof java.lang.reflect.InvocationTargetException
                || current instanceof java.util.concurrent.CompletionException)
                && current.getCause() != null) current = current.getCause();
        return current;
    }

    private static String rootMessage(Throwable error) {
        Throwable root = unwrap(error);
        return root.getMessage() == null ? root.getClass().getSimpleName() : root.getMessage();
    }
}
