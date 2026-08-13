package org.bukkit.plugin.java;

import org.bukkit.plugin.PluginDescriptionFile;
import io.ampznetwork.lunararc.common.LunarArcPlatform;
import io.ampznetwork.lunararc.common.mod.LunarArcRemapper;
import io.ampznetwork.lunararc.common.server.LunarArcPluginLoader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.CodeSource;
import java.security.MessageDigest;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.Map;
import java.util.HexFormat;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Paper/Bukkit plugin classloader for the hybrid runtime. Platform/API classes
 * stay parent-owned; plugin classes are child-first and remapped/cached when
 * legacy Spigot NMS names are detected. Dependency visibility follows declared
 * plugin relationships, including Paper join-classpath rules.
 */
public final class PluginClassLoader extends URLClassLoader {

    private final Map<String, Class<?>> classes = new ConcurrentHashMap<>();
    private final PluginDescriptionFile description;
    private final File dataFolder;
    private final File file;
    private final LunarArcPluginLoader pluginLoader;
    private final ClassLoader libraryLoader;
    private final ClassLoader paperLibraryLoader;
    private final Set<String> noJoinClasspathDependencies;
    private JavaPlugin plugin;
    private final boolean remapNms;
    private final LunarArcRemapper remapper;
    private final Path transformedCacheRoot;

    private static final Map<String, PluginClassLoader> loaders = new ConcurrentHashMap<>();
    private static volatile ClassLoader compatibilityLibraryLoader;

    static {
        ClassLoader.registerAsParallelCapable();
    }

    public PluginClassLoader(LunarArcPluginLoader loader, ClassLoader parent, PluginDescriptionFile description,
            File dataFolder, File file) throws MalformedURLException {
        super(new URL[]{file.toURI().toURL()}, parent);
        this.description = description;
        this.dataFolder = dataFolder;
        this.file = file;
        this.pluginLoader = loader;
        this.remapNms = shouldRemapNms(file);
        this.remapper = new LunarArcRemapper(this.remapNms);
        this.transformedCacheRoot = createTransformedCacheRoot(file, this.remapNms);
        this.libraryLoader = createLibraryLoader(description);
        this.paperLibraryLoader = io.ampznetwork.lunararc.common.server.LunarArcPaperPluginSupport
                .createLibraryLoader(file, description, dataFolder, parent);
        this.noJoinClasspathDependencies = io.ampznetwork.lunararc.common.server.LunarArcPaperPluginSupport
                .noJoinClasspathDependencies(file);
        loaders.put(normalize(description.getName()), this);
        for (String provided : description.getProvides()) {
            loaders.putIfAbsent(normalize(provided), this);
        }
    }

    // Invoked by the JVM when resolving references inside already-loaded plugin classes.
    // URLClassLoader.loadClass() tries parent first, then calls findClass(), so this
    // path already handles most mod-class references automatically via the parent.
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return findClass(name, true);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> loaded = findLoadedClass(name);
            if (loaded == null) {
                if (isPlatformClass(name)) {
                    loaded = loadPlatformClass(name);
                } else {
                    try {
                        loaded = findClass(name, true);
                    } catch (ClassNotFoundException ignored) {
                        loaded = getParent().loadClass(name);
                    }
                }
            }
            if (resolve) resolveClass(loaded);
            return loaded;
        }
    }

    public Class<?> findClass(String name, boolean checkGlobal) throws ClassNotFoundException {
        // 1. Cache hit
        Class<?> result = classes.get(name);
        if (result != null) return result;

        // 2. Paper / Bukkit API → parent (= mod class loader on modded platforms)
        if (isPlatformClass(name)) {
            try { return loadPlatformClass(name); } catch (ClassNotFoundException ignored) {}
        }

        // 3. Plugin's own JAR (with bytecode remapping)
        String path = name.replace('.', '/').concat(".class");
        URL url = findResource(path);
        if (url != null) {
            try (InputStream is = url.openStream()) {
                byte[] original = is.readAllBytes();
                byte[] bytecode = loadOrCreateTransformedClass(path, name, original);

                String pkg = name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : null;
                if (pkg != null && getDefinedPackage(pkg) == null) {
                    definePackage(pkg, null, null, null, null, null, null, null);
                }

                CodeSource cs = new CodeSource(file.toURI().toURL(), (Certificate[]) null);
                ProtectionDomain pd = new ProtectionDomain(cs, null, this, null);
                result = defineClass(name, bytecode, 0, bytecode.length, pd);
                classes.put(name, result);
                return result;
            } catch (IOException e) {
                throw new ClassNotFoundException(name, e);
            }
        }

        // 4. Only declared dependencies share classes with this plugin.
        if (checkGlobal) {
            for (String dependency : visibleDependencies()) {
                PluginClassLoader other = loaders.get(normalize(dependency));
                if (other == null || other == this) continue;
                try {
                    return other.findClass(name, false);
                } catch (ClassNotFoundException ignored) {
                }
            }
        }

        // 5. Legacy Bukkit's JavaPluginLoader exposes classes from other loaded
        //    plugins through a global lookup as a compatibility fallback. This matters
        //    for long-standing integrations such as LuckPerms <-> Vault, where a
        //    plugin-owned child/JarInJar classloader can request Vault API classes
        //    without a direct dependency edge being visible to that nested loader.
        //    Keep declared dependencies first, then fall back to the global plugin
        //    class space (Paper may warn about undeclared access, but it is resolvable).
        if (checkGlobal) {
            for (PluginClassLoader other : new java.util.LinkedHashSet<>(loaders.values())) {
                if (other == null || other == this) continue;
                try {
                    return other.findClass(name, false);
                } catch (ClassNotFoundException ignored) {
                }
            }
        }

        // 6. Modern paper-plugin.yml PluginLoader libraries. The loader is run in
        //    an isolated temporary classloader, matching Paper's contract that loader
        //    statics do not leak into the final plugin classloader.
        if (paperLibraryLoader != null) {
            try { return paperLibraryLoader.loadClass(name); } catch (ClassNotFoundException ignored) {}
        }

        // 5. Paper/Bukkit plugin.yml libraries. Paper resolves these from Maven
        //    Central and exposes them only to the requesting plugin.
        if (libraryLoader != null) {
            try { return libraryLoader.loadClass(name); } catch (ClassNotFoundException ignored) {}
        }

        // 6. LunarArc compatibility libraries. These are kept as nested JARs instead
        //    of being flattened into the NeoForge module (Commons Lang 2 contains the
        //    legacy package org.apache.commons.lang.enum, which Java 9+ modules reject).
        ClassLoader compatibilityLoader = compatibilityLibraryLoader();
        if (compatibilityLoader != null) {
            try { return compatibilityLoader.loadClass(name); } catch (ClassNotFoundException ignored) {}
        }

        // 7. Mod class loader — lets plugins call into mod APIs.
        //    This is the key step that unifies the plugin and mod class spaces,
        //    following the same principle as arclight-common's PluginClassLoader.
        ClassLoader modCL = LunarArcPlatform.getModClassLoader();
        if (modCL != null && modCL != getParent()) {
            try { return modCL.loadClass(name); } catch (ClassNotFoundException ignored) {}
        }

        // 6. Parent as last resort (covers system classes and anything the mod
        //    loader's parent knows about that we haven't already tried).
        try { return getParent().loadClass(name); } catch (ClassNotFoundException ignored) {}

        throw new ClassNotFoundException(name);
    }


    private byte[] loadOrCreateTransformedClass(String resourcePath, String className, byte[] original) throws IOException {
        // Do not mirror class names directly onto the host filesystem. Windows is
        // case-insensitive, while JVM class names are case-sensitive; obfuscated plugins
        // such as LiteBans legitimately contain classes whose names differ only by case.
        // Hash the exact resource path so eB.class and eb.class can never collide.
        Path cached = transformedCacheRoot.resolve("classes")
                .resolve(cacheKey(resourcePath) + ".class").normalize();
        if (!cached.startsWith(transformedCacheRoot)) {
            throw new IOException("Unsafe transformed plugin cache path: " + resourcePath);
        }
        if (Files.isRegularFile(cached)) {
            try {
                byte[] bytes = Files.readAllBytes(cached);
                if (bytes.length >= 8) return bytes;
            } catch (IOException ignored) {
            }
        }

        byte[] transformed = remapper.transform(original, className.replace('.', '/'));
        Files.createDirectories(cached.getParent());
        Path temp = cached.resolveSibling(cached.getFileName() + ".tmp");
        Files.write(temp, transformed);
        try {
            Files.move(temp, cached, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
            Files.move(temp, cached, StandardCopyOption.REPLACE_EXISTING);
        }
        return transformed;
    }


    private static String cacheKey(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static Path createTransformedCacheRoot(File pluginFile, boolean remapNms) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(pluginFile.toPath())) {
                byte[] buffer = new byte[128 * 1024];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    if (read > 0) digest.update(buffer, 0, read);
                }
            }
            digest.update((byte) (remapNms ? 1 : 0));
            digest.update(io.ampznetwork.lunararc.common.server.LunarArcVersionInfo.minecraftVersion()
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8));
            // Bump when LunarArc's built-in plugin compatibility transformations change.
            digest.update("compat-transform-v3".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            // Cache transformed classes against the remapping data as well as the
            // plugin JAR. Otherwise fixing/refreshing mappings can leave stale bytecode
            // (for example TAB still referencing ServerPlayer.c after it was mapped to
            // connection) until the user manually deletes LunarArc's cache.
            ClassLoader owner = PluginClassLoader.class.getClassLoader();
            String mappingBase = "mappings/" + io.ampznetwork.lunararc.common.server.LunarArcVersionInfo.minecraftVersion() + "/";
            for (String resource : new String[]{"paper-reobf.tiny", "plugin-remap.tsv"}) {
                try (InputStream mapping = owner.getResourceAsStream(mappingBase + resource)) {
                    if (mapping != null) {
                        byte[] buffer = new byte[32 * 1024];
                        int count;
                        while ((count = mapping.read(buffer)) >= 0) {
                            if (count > 0) digest.update(buffer, 0, count);
                        }
                    }
                }
            }
            String key = HexFormat.of().formatHex(digest.digest());
            Path root = io.ampznetwork.lunararc.common.LunarArcPaths.transformedPlugins()
                    .resolve(key.substring(0, 24));
            Files.createDirectories(root);
            return root;
        } catch (Exception error) {
            Path fallback = io.ampznetwork.lunararc.common.LunarArcPaths.transformedPlugins()
                    .resolve(normalize(pluginFile.getName()));
            try { Files.createDirectories(fallback); } catch (IOException ignored) {}
            return fallback;
        }
    }

    private Class<?> loadPlatformClass(String name) throws ClassNotFoundException {
        if (name.startsWith("org.bukkit.craftbukkit.") || (this.remapNms && name.startsWith("net.minecraft."))) {
            String mapped = remapper.map(name.replace('.', '/')).replace('/', '.');
            if (!mapped.equals(name)) {
                try {
                    return getParent().loadClass(mapped);
                } catch (ClassNotFoundException ignored) {
                    // Fall back to the original name below.
                }
            }
        }
        return getParent().loadClass(name);
    }

    private static ClassLoader createLibraryLoader(PluginDescriptionFile description) {
        try {
            if (description.getLibraries().isEmpty()) return null;
            return new LibraryLoader(java.util.logging.Logger.getLogger(description.getName())).createLoader(description);
        } catch (Throwable error) {
            throw new IllegalStateException("Could not resolve libraries for " + description.getName(), error);
        }
    }

    private static ClassLoader compatibilityLibraryLoader() {
        ClassLoader existing = compatibilityLibraryLoader;
        if (existing != null) return existing;

        synchronized (PluginClassLoader.class) {
            if (compatibilityLibraryLoader != null) return compatibilityLibraryLoader;

            String resourceName = "META-INF/lunararc-libs/commons-lang-2.6.jar";
            ClassLoader owner = PluginClassLoader.class.getClassLoader();
            try (InputStream input = owner.getResourceAsStream(resourceName)) {
                if (input == null) return null;

                Path directory = io.ampznetwork.lunararc.common.LunarArcPaths.pluginLibraries().resolve("compat");
                Files.createDirectories(directory);
                Path library = directory.resolve("commons-lang-2.6.jar");
                if (!Files.isRegularFile(library) || Files.size(library) == 0L) {
                    Path temp = library.resolveSibling(library.getFileName() + ".tmp");
                    Files.copy(input, temp, StandardCopyOption.REPLACE_EXISTING);
                    try {
                        Files.move(temp, library, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                    } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                        Files.move(temp, library, StandardCopyOption.REPLACE_EXISTING);
                    }
                }

                compatibilityLibraryLoader = new URLClassLoader(
                        new URL[]{library.toUri().toURL()},
                        owner
                );
                return compatibilityLibraryLoader;
            } catch (IOException error) {
                java.util.logging.Logger.getLogger("LunarArc").warning(
                        "Could not expose bundled plugin compatibility libraries: " + error.getMessage());
                return null;
            }
        }
    }

    /**
     * Matches Paper's 1.20.5+ mapping namespace behavior:
     * legacy plugin.yml plugins are Spigot-mapped unless their manifest says
     * otherwise; paper-plugin.yml plugins are Mojang-mapped by default.
     */
    private static boolean shouldRemapNms(File file) {
        try (java.util.jar.JarFile jar = new java.util.jar.JarFile(file)) {
            // A versioned CraftBukkit reference is conclusive evidence that this is
            // legacy Spigot-mapped bytecode, even if a repackager left a misleading
            // Mojang namespace manifest behind. TAB and DecentHolograms 1.21_R1 are
            // examples: without this check ServerPlayer.c reaches the Mojang runtime.
            String legacyNeedle = "org/bukkit/craftbukkit/"
                    + io.ampznetwork.lunararc.common.server.LunarArcVersionInfo.craftBukkitPackage() + "/";
            var entries = jar.entries();
            while (entries.hasMoreElements()) {
                java.util.jar.JarEntry entry = entries.nextElement();
                if (entry.isDirectory() || !entry.getName().endsWith(".class")) continue;
                // Version-specific adapter packages (TAB, DecentHolograms, etc.)
                // are compiled against legacy CraftBukkit/Spigot NMS names even
                // when the class itself doesn't reference a CraftBukkit type.
                if (entry.getName().contains("/" + io.ampznetwork.lunararc.common.server.LunarArcVersionInfo.craftBukkitPackage() + "/")) {
                    return true;
                }
                try (InputStream in = jar.getInputStream(entry)) {
                    byte[] bytes = in.readAllBytes();
                    if (new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1).contains(legacyNeedle)) {
                        return true;
                    }
                }
            }

            java.util.jar.Manifest manifest = jar.getManifest();
            if (manifest != null) {
                String namespace = manifest.getMainAttributes().getValue("paperweight-mappings-namespace");
                if (namespace != null) {
                    if ("mojang".equalsIgnoreCase(namespace)) return false;
                    if ("spigot".equalsIgnoreCase(namespace)) return true;
                }
            }
            return jar.getJarEntry("paper-plugin.yml") == null;
        } catch (IOException ignored) {
            return true;
        }
    }

    private static boolean isPlatformClass(String name) {
        return name.startsWith("java.")
                || name.startsWith("javax.")
                || name.startsWith("jdk.")
                || name.startsWith("sun.")
                || name.startsWith("org.bukkit.")
                || name.startsWith("com.destroystokyo.paper.")
                || name.startsWith("io.papermc.paper.")
                || name.startsWith("net.kyori.adventure.")
                || name.startsWith("net.kyori.examination.")
                || name.startsWith("com.mojang.")
                || name.startsWith("net.minecraft.");
    }

    @Override
    public URL getResource(String name) {
        URL url = findResource(name);
        if (url == null) url = getParent().getResource(name);
        if (url == null) {
            for (String dependency : visibleDependencies()) {
                PluginClassLoader other = loaders.get(normalize(dependency));
                if (other == null || other == this) continue;
                url = other.findResource(name);
                if (url != null) break;
            }
        }
        if (url == null && paperLibraryLoader != null) url = paperLibraryLoader.getResource(name);
        if (url == null && libraryLoader != null) url = libraryLoader.getResource(name);
        if (url == null) {
            ClassLoader compatibility = compatibilityLibraryLoader();
            if (compatibility != null) url = compatibility.getResource(name);
        }
        return url;
    }


    private Set<String> visibleDependencies() {
        Set<String> dependencies = new LinkedHashSet<>();
        for (String dependency : description.getDepend()) {
            if (!noJoinClasspathDependencies.contains(normalize(dependency))) dependencies.add(dependency);
        }
        for (String dependency : description.getSoftDepend()) {
            if (!noJoinClasspathDependencies.contains(normalize(dependency))) dependencies.add(dependency);
        }
        return dependencies;
    }

    private static String normalize(String name) {
        return name.replace(' ', '_').toLowerCase(java.util.Locale.ROOT);
    }

    JavaPlugin getPluginInstance() {
        return plugin;
    }

    LunarArcPluginLoader getPluginLoaderInstance() {
        return pluginLoader;
    }

    public synchronized void initialize(JavaPlugin plugin) {
        if (this.plugin != null) throw new IllegalStateException("Plugin already initialized!");

        this.plugin = plugin;
        plugin.init(
                pluginLoader.getServerInstance(),
                description,
                dataFolder,
                file,
                this,
                new io.ampznetwork.lunararc.common.server.LunarArcPluginMeta(description),
                io.ampznetwork.lunararc.common.server.LunarArcLogger.getLogger(description.getName())
        );
    }

    @Override
    public void close() throws IOException {
        IOException failure = null;
        try {
            super.close();
        } catch (IOException error) {
            failure = error;
        } finally {
            loaders.entrySet().removeIf(entry -> entry.getValue() == this);
            if (paperLibraryLoader instanceof java.io.Closeable closeable) {
                try { closeable.close(); } catch (IOException error) { if (failure == null) failure = error; }
            }
            if (libraryLoader instanceof java.io.Closeable closeable) {
                try { closeable.close(); } catch (IOException error) { if (failure == null) failure = error; }
            }
        }
        if (failure != null) throw failure;
    }
}
