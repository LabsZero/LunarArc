package io.ampznetwork.lunararc.neoforge.locator;

import net.neoforged.neoforgespi.locating.IModFile;
import net.neoforged.neoforgespi.locating.IModLocator;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Registered via ServiceLoader so FML discovers LunarArc when the self-JAR is
 * on the system classloader (injected by LunarArcAgent in same-JVM mode).
 *
 * scanMods() returns the self-JAR path using FML internals via reflection so
 * NeoForge does not need a bridge copy in ./mods/.
 */
public class LunarArcModLocator implements IModLocator {

    @Override
    public List<ModFileOrException> scanMods() {
        try {
            Path selfJar = getSelfJarPath();
            if (selfJar == null) {
                System.err.println("[LunarArc] ModLocator: could not determine self-JAR path");
                return List.of();
            }

            // cpw.mods.jarhandling.SecureJar.from(Path...)
            Class<?> secureJarCls = Class.forName("cpw.mods.jarhandling.SecureJar");
            Method fromMethod = secureJarCls.getMethod("from", Path[].class);
            Object secureJar = fromMethod.invoke(null, (Object) new Path[]{selfJar});

            // Try: net.neoforged.fml.loading.moddiscovery.ModFile.newFMLInstance(SecureJar, IModLocator)
            Object modFile = tryBuildModFile(secureJar);
            if (modFile == null) {
                System.err.println("[LunarArc] ModLocator: could not build ModFile from self-JAR");
                return List.of();
            }

            // Wrap in ModFileOrException
            Object mfoe = buildModFileOrException(modFile);
            if (mfoe == null) return List.of();

            @SuppressWarnings("unchecked")
            List<ModFileOrException> result = (List<ModFileOrException>) List.of(mfoe);
            System.out.println("[LunarArc] ModLocator: self-registered from " + selfJar);
            return result;
        } catch (Throwable t) {
            System.err.println("[LunarArc] ModLocator scanMods failed (bridge JAR fallback): " + t);
            return List.of();
        }
    }

    private static Path getSelfJarPath() {
        try {
            java.security.CodeSource cs = LunarArcModLocator.class.getProtectionDomain().getCodeSource();
            if (cs == null) return null;
            java.net.URL loc = cs.getLocation();
            if (loc == null) return null;
            Path p = Paths.get(loc.toURI());
            return Files.exists(p) && p.toString().endsWith(".jar") ? p : null;
        } catch (Throwable t) {
            return null;
        }
    }

    private Object tryBuildModFile(Object secureJar) {
        try {
            Class<?> modFileCls = Class.forName("net.neoforged.fml.loading.moddiscovery.ModFile");
            // Try 2-arg: newFMLInstance(SecureJar, IModLocator)
            for (Method m : modFileCls.getMethods()) {
                if ("newFMLInstance".equals(m.getName()) && m.getParameterCount() == 2) {
                    return m.invoke(null, secureJar, this);
                }
            }
            // Try 3-arg: newFMLInstance(SecureJar, IModLocator, BiFunction/Function)
            for (Method m : modFileCls.getMethods()) {
                if ("newFMLInstance".equals(m.getName()) && m.getParameterCount() == 3) {
                    return m.invoke(null, secureJar, this, null);
                }
            }
            // Try constructor: ModFile(SecureJar, IModLocator, IModFileInfo)
            for (Constructor<?> ctor : modFileCls.getDeclaredConstructors()) {
                if (ctor.getParameterCount() == 3) {
                    ctor.setAccessible(true);
                    // Build a ModJarMetadata for the IModFileInfo slot
                    Object meta = tryBuildModJarMetadata(secureJar);
                    return ctor.newInstance(secureJar, this, meta);
                }
            }
        } catch (Throwable t) {
            System.err.println("[LunarArc] ModLocator: ModFile build attempt failed: " + t);
        }
        return null;
    }

    private Object tryBuildModJarMetadata(Object secureJar) {
        try {
            Class<?> metaCls = Class.forName("net.neoforged.fml.loading.moddiscovery.ModJarMetadata");
            for (Constructor<?> ctor : metaCls.getDeclaredConstructors()) {
                ctor.setAccessible(true);
                if (ctor.getParameterCount() == 1) {
                    return ctor.newInstance(secureJar);
                }
                if (ctor.getParameterCount() == 0) {
                    return ctor.newInstance();
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private Object buildModFileOrException(Object modFile) {
        try {
            // Try the real FML class
            Class<?> mfoeCls = Class.forName("net.neoforged.neoforgespi.locating.IModLocator$ModFileOrException");
            for (Constructor<?> ctor : mfoeCls.getDeclaredConstructors()) {
                if (ctor.getParameterCount() == 2) {
                    ctor.setAccessible(true);
                    return ctor.newInstance(modFile, null);
                }
            }
        } catch (Throwable t) {
            System.err.println("[LunarArc] ModLocator: ModFileOrException build failed: " + t);
        }
        return null;
    }

    @Override
    public String name() {
        return "lunararc-locator";
    }

    @Override
    public void scanFile(IModFile modFile, Consumer<Path> pathConsumer) {
    }

    @Override
    public void initArguments(Map<String, ?> arguments) {
    }

    @Override
    public boolean isValid(IModFile modFile) {
        return true;
    }
}
