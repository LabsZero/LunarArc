package io.papermc.paper.plugin.loader.library.impl;

import io.papermc.paper.plugin.loader.library.ClassPathLibrary;
import io.papermc.paper.plugin.loader.library.LibraryLoadingException;
import io.papermc.paper.plugin.loader.library.LibraryStore;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.eclipse.aether.internal.impl.synccontext.named.NameMapper;
import org.eclipse.aether.internal.impl.synccontext.named.NameMappers;
import org.eclipse.aether.supplier.RepositorySystemSupplier;


public class MavenLibraryResolver implements ClassPathLibrary {
    public static final String MAVEN_CENTRAL_DEFAULT_MIRROR = defaultCentral();
    private final RepositorySystem repository;
    private final DefaultRepositorySystemSession session;
    private final List<RemoteRepository> repositories = new ArrayList<>();
    private final List<Dependency> dependencies = new ArrayList<>();

    public MavenLibraryResolver() {
        this.repository = new RepositorySystemSupplier() {
            @Override
            protected Map<String, NameMapper> getNameMappers() {


                HashMap<String, NameMapper> result = new HashMap<>();
                result.put(NameMappers.STATIC_NAME, NameMappers.staticNameMapper());
                result.put(NameMappers.GAV_NAME, NameMappers.gavNameMapper());
                result.put(NameMappers.FILE_GAV_NAME, NameMappers.fileGavNameMapper());
                result.put(NameMappers.FILE_HGAV_NAME, NameMappers.fileHashingGavNameMapper());
                return result;
            }
        }.get();
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
        } finally {
            // RepositorySystemSupplier owns named-lock/lifecycle resources. Paper's
            // classpath builder resolves each library set once, so release them as soon
            // as the concrete library paths have been produced.
            repository.shutdown();
        }
    }
    private static String defaultCentral() {
        String central = System.getenv("PAPER_DEFAULT_CENTRAL_REPOSITORY");
        if (central == null) central = System.getProperty("org.bukkit.plugin.java.LibraryLoader.centralURL");
        return central != null ? central : "https://maven-central.storage-download.googleapis.com/maven2";
    }
}
