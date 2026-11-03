package io.lunararcdevs.lunararc.common.server;

import io.lunararcdevs.lunararc.api.Unsafe;
import org.bukkit.plugin.java.PluginClassLoader;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class LunarArcModPluginBridge {
    private static final CopyOnWriteArrayList<PluginClassLoader> PLUGINS = new CopyOnWriteArrayList<>();
    private static final Set<String> MISSING = ConcurrentHashMap.newKeySet();
    private static final ThreadLocal<Boolean> SEARCHING = new ThreadLocal<>();

    private LunarArcModPluginBridge() {}

    public static void register(PluginClassLoader loader) {
        PLUGINS.addIfAbsent(loader);
        MISSING.clear();
    }

    public static void unregister(PluginClassLoader loader) {
        PLUGINS.remove(loader);
    }

    private static Field field(Class<?> type, String name) {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
            }
        }
        return null;
    }

    private static Object read(Object target, Field field) {
        return field == null ? null : Unsafe.getObject(target, Unsafe.objectFieldOffset(field));
    }

    public static void install(ClassLoader modLoader) {
        Class<?> type = modLoader.getClass();
        boolean module = false;
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            if ("cpw.mods.cl.ModuleClassLoader".equals(c.getName())) module = true;
        }
        Field fallback = field(type, "fallbackClassLoader");
        if (!module || fallback == null) return;
        try {
            java.util.List<ClassLoader> delegates = new java.util.ArrayList<>();
            ClassLoader previous = (ClassLoader) read(modLoader, fallback);
            if (previous != null) {
                delegates.add(previous);
            } else {
                ClassLoader parent = (ClassLoader) read(modLoader, field(type, "parent"));
                if (parent != null) delegates.add(parent);
                Object all = read(modLoader, field(type, "allParentLoaders"));
                if (all instanceof java.util.Collection<?> loaders) {
                    for (Object loader : loaders) if (loader instanceof ClassLoader cl && !delegates.contains(cl)) delegates.add(cl);
                }
            }
            Method setter = type.getMethod("setFallbackClassLoader", ClassLoader.class);
            setter.invoke(modLoader, new PluginFallback(delegates));
        } catch (ReflectiveOperationException | RuntimeException unavailable) {
            org.slf4j.LoggerFactory.getLogger("LunarArc").warn("Mods will not be able to load classes from plugins: {}", unavailable.toString());
        }
    }

    private static Class<?> find(String name) throws ClassNotFoundException {
        if (PLUGINS.isEmpty() || MISSING.contains(name) || SEARCHING.get() != null) throw new ClassNotFoundException(name);
        SEARCHING.set(Boolean.TRUE);
        try {
            for (PluginClassLoader plugin : PLUGINS) {
                try {
                    return plugin.findClassFromOwnSpace(name);
                } catch (ClassNotFoundException ignored) {
                }
            }
        } finally {
            SEARCHING.remove();
        }
        MISSING.add(name);
        throw new ClassNotFoundException(name);
    }

    private static final class PluginFallback extends ClassLoader {
        private final java.util.List<ClassLoader> delegates;

        private PluginFallback(java.util.List<ClassLoader> delegates) {
            super(null);
            this.delegates = delegates;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            for (ClassLoader delegate : delegates) {
                try {
                    return delegate.loadClass(name);
                } catch (ClassNotFoundException ignored) {
                }
            }
            return find(name);
        }

        @Override
        protected java.net.URL findResource(String name) {
            for (ClassLoader delegate : delegates) {
                java.net.URL url = delegate.getResource(name);
                if (url != null) return url;
            }
            return null;
        }

        @Override
        protected java.util.Enumeration<java.net.URL> findResources(String name) throws java.io.IOException {
            java.util.List<java.net.URL> found = new java.util.ArrayList<>();
            for (ClassLoader delegate : delegates) found.addAll(java.util.Collections.list(delegate.getResources(name)));
            return java.util.Collections.enumeration(found);
        }
    }
}
