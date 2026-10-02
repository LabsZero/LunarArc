package io.lunararcdevs.lunararc.neoforge.mod;

import cpw.mods.jarhandling.JarContents;
import cpw.mods.jarhandling.JarContentsBuilder;
import cpw.mods.jarhandling.SecureJar;
import cpw.mods.jarhandling.impl.JarContentsImpl;
import io.lunararcdevs.lunararc.api.Unsafe;
import net.neoforged.fml.loading.moddiscovery.readers.JarModsDotTomlModFileReader;
import net.neoforged.neoforgespi.locating.IModFile;
import net.neoforged.neoforgespi.locating.IModFileReader;
import net.neoforged.neoforgespi.locating.IOrderedProvider;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.VarHandle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

public final class LunarArcModFileReader_NeoForge implements IModFileReader {
    private static final Logger LOGGER = LoggerFactory.getLogger("LunarArc");
    private static final String SERVICES = "META-INF/services/";
    private static final VarHandle PACKAGES = cacheHandle("packages", Set.class);
    private static final VarHandle PROVIDERS = cacheHandle("providers", List.class);

    private final Set<String> libraryPackages = collectLibraryPackages();
    private final Set<String> reported = ConcurrentHashMap.newKeySet();
    private final IModFileReader delegate = new JarModsDotTomlModFileReader();

    private static VarHandle cacheHandle(String field, Class<?> type) {
        try {
            return Unsafe.lookup().findVarHandle(JarContentsImpl.class, field, type);
        } catch (ReflectiveOperationException unavailable) {
            LOGGER.warn("Could not access FML's jar contents cache; nested libraries that clash with LunarArc's own cannot be adjusted", unavailable);
            return null;
        }
    }

    private static Set<String> collectLibraryPackages() {
        Set<String> packages = new HashSet<>();
        for (String name : LunarArcModFileReader_NeoForge.class.getModule().getPackages()) {
            if (!name.startsWith("io.lunararcdevs.") && !name.startsWith("META-INF")) packages.add(name);
        }
        return packages;
    }

    private static boolean ownedByLunarArc(Path path) {
        return path.toString().replace('\\', '/').contains("/.lunararc/");
    }

    private static String stripLeadingSlash(String entry) {
        return entry.startsWith("/") ? entry.substring(1) : entry;
    }

    private static String packageOf(String entry) {
        String normalized = stripLeadingSlash(entry);
        int slash = normalized.lastIndexOf('/');
        return slash < 0 ? "" : normalized.substring(0, slash).replace('/', '.');
    }

    private static String packageOfClass(String className) {
        return className.substring(0, Math.max(0, className.lastIndexOf('.')));
    }

    private static String describe(Path primary, ModFileDiscoveryAttributes attributes) {
        Path name = primary.getFileName();
        if (name != null && !name.toString().isEmpty()) return name.toString();
        return attributes.parent() == null ? primary.toString() : "a library nested in " + attributes.parent().getFileName();
    }

    @Override
    public int getPriority() {
        return IOrderedProvider.HIGHEST_SYSTEM_PRIORITY;
    }

    @Override
    public IModFile read(JarContents jar, ModFileDiscoveryAttributes attributes) {
        Path primary = jar.getPrimaryPath();
        if (primary == null || ownedByLunarArc(primary)) return null;

        Set<String> clashing = new TreeSet<>();
        for (String name : jar.getPackages()) {
            if (libraryPackages.contains(name)) clashing.add(name);
        }
        Set<String> staleServiceFiles = new TreeSet<>();
        for (SecureJar.Provider provider : jar.getMetaInfServices()) {
            for (String implementation : provider.providers()) {
                String implementationPackage = packageOfClass(implementation);
                if (libraryPackages.contains(implementationPackage)) {
                    staleServiceFiles.add(SERVICES + provider.serviceName());
                    clashing.add(implementationPackage);
                    break;
                }
            }
        }
        if (clashing.isEmpty()) return null;

        IModFile modFile = readWithoutClashingEntries(primary, attributes, clashing, staleServiceFiles);
        if (modFile == null && !hideInCache(jar, clashing)) return null;
        String description = describe(primary, attributes);
        String examples = String.join(", ", clashing.stream().limit(3).toList());
        if (reported.add(description + ": " + examples)) {
            LOGGER.info("{} declares {} package(s) LunarArc already provides ({}); hiding them so it uses LunarArc's copy", description, clashing.size(), examples);
        }
        return modFile;
    }

    private IModFile readWithoutClashingEntries(Path primary, ModFileDiscoveryAttributes attributes, Set<String> clashing, Set<String> staleServiceFiles) {
        try {
            if (!Files.isRegularFile(primary)) return null;
            JarContents filtered = new JarContentsBuilder()
                    .paths(primary)
                    .pathFilter((entry, base) -> !clashing.contains(packageOf(entry)) && !staleServiceFiles.contains(stripLeadingSlash(entry)))
                    .build();
            IModFile modFile = delegate.read(filtered, attributes);
            if (modFile == null) filtered.close();
            return modFile;
        } catch (Throwable failure) {
            LOGGER.warn("Could not reopen {} without the packages that clash with LunarArc's own", primary.getFileName(), failure);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static boolean hideInCache(JarContents jar, Set<String> clashing) {
        if (PACKAGES == null || PROVIDERS == null || !(jar instanceof JarContentsImpl impl)) return false;
        try {
            impl.getPackages();
            impl.getMetaInfServices();
            Set<String> packages = new HashSet<>((Set<String>) PACKAGES.get(impl));
            packages.removeAll(clashing);
            PACKAGES.set(impl, packages);
            List<SecureJar.Provider> providers = ((List<SecureJar.Provider>) PROVIDERS.get(impl)).stream()
                    .filter(provider -> provider.providers().stream().noneMatch(implementation -> clashing.contains(packageOfClass(implementation))))
                    .toList();
            PROVIDERS.set(impl, providers);
            return true;
        } catch (Throwable failure) {
            LOGGER.warn("Could not adjust the packages of a library that clashes with LunarArc's own", failure);
            return false;
        }
    }

    @Override
    public String toString() {
        return "lunararc-duplicate-libraries";
    }
}
