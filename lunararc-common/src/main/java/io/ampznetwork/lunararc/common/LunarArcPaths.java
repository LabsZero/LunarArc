package io.ampznetwork.lunararc.common;

import java.nio.file.Files;
import java.nio.file.Path;

public final class LunarArcPaths {
    private LunarArcPaths() {}

    public static Path home() {
        String configured = System.getProperty("lunararc.home");
        Path path = configured == null || configured.isBlank()
                ? Path.of(".lunararc").toAbsolutePath().normalize()
                : Path.of(configured).toAbsolutePath().normalize();
        try { Files.createDirectories(path); } catch (Exception ignored) {}
        return path;
    }

    public static Path pluginLibraries() {
        Path path = home().resolve("libraries/plugins");
        try { Files.createDirectories(path); } catch (Exception ignored) {}
        return path;
    }

    public static Path transformedPlugins() {
        return ensure(home().resolve("cache/transformed-plugins"));
    }

    public static Path mappings() {
        return ensure(home().resolve("mappings"));
    }

    public static Path cache() {
        return ensure(home().resolve("cache"));
    }

    public static Path runtime() {
        return ensure(home().resolve("runtime"));
    }

    private static Path ensure(Path path) {
        try { Files.createDirectories(path); } catch (Exception ignored) {}
        return path;
    }
}
