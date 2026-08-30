package io.ampznetwork.lunararc.common.server;

import io.papermc.paper.plugin.loader.library.LibraryLoadingException;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.bukkit.plugin.PluginDescriptionFile;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.artifact.JavaScopes;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * Concrete resolver for Bukkit/Paper {@code plugin.yml -> libraries} entries.
 *
 * <p>Paper normally implements this in its server-side LibraryLoader. LunarArc
 * deliberately does not copy Paper's server runtime, so the hybrid owns this
 * small resolver directly and reuses the same Maven Resolver stack already
 * required by Paper PluginLoader implementations.</p>
 */
public final class LunarArcLegacyLibraryResolver {
    private LunarArcLegacyLibraryResolver() {}

    public static ClassLoader create(PluginDescriptionFile description, ClassLoader parent) {
        Objects.requireNonNull(description, "description");
        List<String> declared = description.getLibraries();
        if (declared == null || declared.isEmpty()) return null;

        MavenLibraryResolver resolver = new MavenLibraryResolver();
        resolver.addRepository(new RemoteRepository.Builder(
                "central", "default", MavenLibraryResolver.MAVEN_CENTRAL_DEFAULT_MIRROR).build());
        int count = 0;
        for (String coordinates : declared) {
            if (coordinates == null || coordinates.isBlank()) continue;
            try {
                resolver.addDependency(new Dependency(new DefaultArtifact(coordinates.trim()), JavaScopes.RUNTIME));
                count++;
            } catch (RuntimeException malformed) {
                throw new IllegalArgumentException("Invalid library coordinates '" + coordinates
                        + "' declared by " + description.getName(), malformed);
            }
        }
        if (count == 0) return null;

        LinkedHashSet<Path> paths = new LinkedHashSet<>();
        try {
            resolver.register(library -> {
                Path path = Objects.requireNonNull(library, "library").toAbsolutePath().normalize();
                if (!Files.isRegularFile(path)) {
                    throw new IllegalArgumentException("Resolved plugin library is not a file: " + path);
                }
                paths.add(path);
            });
        } catch (LibraryLoadingException error) {
            throw new IllegalStateException("Could not resolve libraries for " + description.getName(), error);
        }
        if (paths.isEmpty()) return null;
        URL[] urls = paths.stream().map(path -> {
            try {
                return path.toUri().toURL();
            } catch (java.net.MalformedURLException impossible) {
                throw new IllegalArgumentException(impossible);
            }
        }).toArray(URL[]::new);
        return new URLClassLoader(urls, parent);
    }
}
