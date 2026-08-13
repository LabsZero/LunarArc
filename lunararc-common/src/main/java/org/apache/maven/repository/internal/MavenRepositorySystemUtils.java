package org.apache.maven.repository.internal;

/**
 * LunarArc runtime bootstrap for Maven Resolver when Paper's modern
 * MavenLibraryResolver is used outside Maven/Paper's normal bootstrap.
 */
public final class MavenRepositorySystemUtils {
    private MavenRepositorySystemUtils() {}

    public static org.eclipse.aether.impl.DefaultServiceLocator newServiceLocator() {
        org.eclipse.aether.impl.DefaultServiceLocator locator = new org.eclipse.aether.impl.DefaultServiceLocator();
        locator.addService(org.eclipse.aether.RepositorySystem.class,
                org.eclipse.aether.internal.impl.DefaultRepositorySystem.class);
        locator.addService(org.eclipse.aether.impl.ArtifactDescriptorReader.class,
                DefaultArtifactDescriptorReader.class);
        locator.addService(org.eclipse.aether.impl.VersionResolver.class,
                DefaultVersionResolver.class);
        locator.addService(org.eclipse.aether.impl.VersionRangeResolver.class,
                DefaultVersionRangeResolver.class);
        locator.addService(org.eclipse.aether.spi.connector.RepositoryConnectorFactory.class,
                org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory.class);
        try {
            locator.addService(org.eclipse.aether.spi.connector.transport.TransporterFactory.class,
                    org.eclipse.aether.transport.file.FileTransporterFactory.class);
        } catch (Throwable ignored) {}
        locator.addService(org.eclipse.aether.spi.connector.transport.TransporterFactory.class,
                org.eclipse.aether.transport.http.HttpTransporterFactory.class);
        return locator;
    }

    public static org.eclipse.aether.DefaultRepositorySystemSession newSession() {
        return new org.eclipse.aether.DefaultRepositorySystemSession();
    }
}
