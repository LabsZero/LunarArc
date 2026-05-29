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
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * ClassLoader for Bukkit plugins on a modded server.
 *
 * Class resolution order (mirrors Arclight's unified class-space approach):
 *
 *  1. Already-loaded cache — fast path.
 *  2. Paper / Bukkit API classes → parent (which IS the mod class loader on
 *     modded platforms).  CraftBukkit versioned names are remapped first.
 *  3. Plugin's own JAR — bytecode is transformed by LunarArcRemapper so legacy
 *     version-specific references (e.g. v1_16_R3) are rewritten to the current
 *     target version.
 *  4. Other plugin class loaders — shared plugin class space so inter-plugin
 *     references work without explicit depend/softdepend entries on the classpath.
 *  5. Mod class loader — the critical step that was previously missing.
 *     Delegating here lets plugins call into mod APIs at runtime, fixing the
 *     "plugins show as loaded but don't function in-game" symptom.
 *  6. Parent class loader as final fallback.
 */
public final class PluginClassLoader extends URLClassLoader {

    private final Map<String, Class<?>> classes = new ConcurrentHashMap<>();
    private final PluginDescriptionFile description;
    private final File dataFolder;
    private final File file;
    private JavaPlugin plugin;
    private final LunarArcRemapper remapper = new LunarArcRemapper();

    // Global registry of all active plugin class loaders — enables cross-plugin
    // class sharing without explicit classpath dependencies.
    private static final List<PluginClassLoader> loaders = new CopyOnWriteArrayList<>();

    static {
        ClassLoader.registerAsParallelCapable();
    }

    public PluginClassLoader(LunarArcPluginLoader loader, ClassLoader parent, PluginDescriptionFile description,
            File dataFolder, File file) throws MalformedURLException {
        super(new URL[]{file.toURI().toURL()}, parent);
        this.description = description;
        this.dataFolder = dataFolder;
        this.file = file;
        loaders.add(this);
    }

    // Invoked by the JVM when resolving references inside already-loaded plugin classes.
    // URLClassLoader.loadClass() tries parent first, then calls findClass(), so this
    // path already handles most mod-class references automatically via the parent.
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return findClass(name, true);
    }

    public Class<?> findClass(String name, boolean checkGlobal) throws ClassNotFoundException {
        // 1. Cache hit
        Class<?> result = classes.get(name);
        if (result != null) return result;

        // 2. Paper / Bukkit API → parent (= mod class loader on modded platforms)
        if (isPaperApiClass(name)) {
            if (name.startsWith("org.bukkit.craftbukkit.")) {
                // Remap versioned CraftBukkit references to the current target version.
                String mapped = remapper.map(name.replace('.', '/')).replace('/', '.');
                if (!mapped.equals(name)) {
                    try { return getParent().loadClass(mapped); } catch (ClassNotFoundException ignored) {}
                }
            }
            try { return getParent().loadClass(name); } catch (ClassNotFoundException ignored) {}
        }

        // 3. Plugin's own JAR (with bytecode remapping)
        String path = name.replace('.', '/').concat(".class");
        URL url = findResource(path);
        if (url != null) {
            try (InputStream is = url.openStream()) {
                byte[] bytecode = remapper.transform(is.readAllBytes());

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

        // 4. Other plugins — shared class space
        if (checkGlobal) {
            for (PluginClassLoader other : loaders) {
                if (other == this) continue;
                try { return other.findClass(name, false); } catch (ClassNotFoundException ignored) {}
            }
        }

        // 5. Mod class loader — lets plugins call into mod APIs.
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

    /** Returns true for classes that should be loaded from the platform (Paper/Bukkit API). */
    private static boolean isPaperApiClass(String name) {
        return name.startsWith("org.bukkit.")
                || name.startsWith("com.destroystokyo.paper.")
                || name.startsWith("io.papermc.paper.")
                || name.startsWith("net.kyori.")
                || name.startsWith("io.papermc.")
                || name.startsWith("com.mojang.")
                || name.startsWith("net.minecraft.");
    }

    @Override
    public URL getResource(String name) {
        URL url = findResource(name);
        if (url == null) url = getParent().getResource(name);
        if (url == null) {
            for (PluginClassLoader other : loaders) {
                if (other == this) continue;
                url = other.findResource(name);
                if (url != null) break;
            }
        }
        return url;
    }

    public synchronized void initialize(JavaPlugin plugin) {
        if (this.plugin != null) throw new IllegalStateException("Plugin already initialized!");
        this.plugin = plugin;
    }

    @Override
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            loaders.remove(this);
        }
    }
}
