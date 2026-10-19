package io.lunararcdevs.lunararc.common.util;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.slf4j.Logger;

/**
 * Developer aid ({@code -Dlunararc.auditMixins=true}): loads and initializes every LunarArc class so that a missing
 * runtime library (a {@code NoClassDefFoundError} that would otherwise only appear when a player triggers that code path)
 * shows up at startup instead.
 */
public final class LunarArcClassProbe {
    private LunarArcClassProbe() {
    }

    public static void run(final Logger log) {
        final Path jar;
        try {
            jar = Paths.get(LunarArcClassProbe.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (final URISyntaxException | RuntimeException e) {
            log.info("Class probe skipped: cannot locate the LunarArc jar ({})", e.toString());
            return;
        }
        final ClassLoader loader = LunarArcClassProbe.class.getClassLoader();
        final Map<String, String> problems = new TreeMap<>();
        int checked = 0;
        try (JarFile file = new JarFile(jar.toFile())) {
            final Enumeration<JarEntry> entries = file.entries();
            while (entries.hasMoreElements()) {
                final String name = entries.nextElement().getName();
                if (!name.endsWith(".class") || name.contains("/mixin/") || name.endsWith("package-info.class")
                    || name.endsWith("module-info.class") || !(name.startsWith("org/bukkit/") || name.startsWith("io/papermc/")
                    || name.startsWith("com/destroystokyo/") || name.startsWith("io/lunararcdevs/"))) {
                    continue;
                }
                checked++;
                final String className = name.substring(0, name.length() - 6).replace('/', '.');
                try {
                    Class.forName(className, true, loader);
                } catch (final Throwable failure) {
                    Throwable root = failure;
                    while (root.getCause() != null && root.getCause() != root) {
                        root = root.getCause();
                    }
                    final String what = root instanceof ClassNotFoundException || root instanceof NoClassDefFoundError
                        ? "missing " + root.getMessage() : root.getClass().getSimpleName() + ": " + root.getMessage();
                    problems.putIfAbsent(what, className);
                }
            }
        } catch (final Exception e) {
            log.info("Class probe stopped: {}", e.toString());
            return;
        }
        log.info("Class probe checked {} classes, {} distinct problems", checked, problems.size());
        problems.forEach((what, example) -> log.warn("Class probe: {} (first seen loading {})", what, example));
    }
}
