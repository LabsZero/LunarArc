package io.papermc.paper.plugin.loader.library.impl;

import io.papermc.paper.plugin.loader.library.ClassPathLibrary;
import io.papermc.paper.plugin.loader.library.LibraryLoadingException;
import io.papermc.paper.plugin.loader.library.LibraryStore;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.supplier.RepositorySystemSupplier;

/** Paper-compatible resolver using Resolver's supported supplier instead of a
 * service locator whose component graph is incomplete under ModLauncher. */
public class MavenLibraryResolver implements ClassPathLibrary {
    public static final String MAVEN_CENTRAL_DEFAULT_MIRROR = defaultCentral();
    private final RepositorySystem repository;
    private final DefaultRepositorySystemSession session;
    private final List<RemoteRepository> repositories = new ArrayList<>();
    private final List<Dependency> dependencies = new ArrayList<>();

    public MavenLibraryResolver() {
        this.repository = new RepositorySystemSupplier().get();
        if (this.repository == null) throw new IllegalStateException("Maven Resolver did not create a RepositorySystem");
        this.session = MavenRepositorySystemUtils.newSession();
        this.session.setSystemProperties(System.getProperties());
        this.session.setChecksumPolicy(RepositoryPolicy.CHECKSUM_POLICY_FAIL);
        this.session.setLocalRepositoryManager(this.repository.newLocalRepositoryManager(this.session, new LocalRepository("libraries")));
    }
    public void addDependency(Dependency dependency) { dependencies.add(dependency); }
    public void addRepository(RemoteRepository repository) { repositories.add(repository); }
    @Override public void register(LibraryStore store) throws LibraryLoadingException {
        try {
            List<RemoteRepository> repos = repository.newResolutionRepositories(session, repositories);
            DependencyResult result = repository.resolveDependencies(session,
                    new DependencyRequest(new CollectRequest((Dependency) null, dependencies, repos), null));
            for (ArtifactResult artifact : result.getArtifactResults()) {
                File file = artifact.getArtifact().getFile();
                if (file != null) store.addLibrary(file.toPath());
            }
        } catch (DependencyResolutionException ex) {
            throw new LibraryLoadingException("Error resolving libraries", ex);
        }
    }
    private static String defaultCentral() {
        String central = System.getenv("PAPER_DEFAULT_CENTRAL_REPOSITORY");
        if (central == null) central = System.getProperty("org.bukkit.plugin.java.LibraryLoader.centralURL");
        return central != null ? central : "https://maven-central.storage-download.googleapis.com/maven2";
    }
}
