package io.ampznetwork.lunararc.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Owns LunarArc's disposable runtime directory. Everything below .lunararc may
 * be deleted by the user and is reconstructed from the server jar on the next
 * launch.
 */
public final class LunarArcRuntime {
    private LunarArcRuntime() {}

    public record Layout(Path root, Path runtime, Path runtimeClasses, Path libraries,
                         Path pluginLibraries, Path mappings, Path cache, Path downloads,
                         Path state, Path coreJar) {}

    public static Layout prepare(Path workingDir, Path selfJar, Properties versions, String platform) throws Exception {
        Path root = workingDir.resolve(".lunararc").toAbsolutePath().normalize();
        Layout layout = new Layout(
                root,
                root.resolve("runtime"),
                root.resolve("runtime/classes"),
                root.resolve("libraries"),
                root.resolve("libraries/plugins"),
                root.resolve("mappings"),
                root.resolve("cache"),
                root.resolve("downloads"),
                root.resolve("state"),
                root.resolve("runtime/lunararc-core.jar")
        );

        Files.createDirectories(layout.runtime());
        Files.createDirectories(layout.libraries());
        Files.createDirectories(layout.pluginLibraries());
        Files.createDirectories(layout.mappings());
        Files.createDirectories(layout.cache().resolve("transformed-plugins"));
        Files.createDirectories(layout.downloads());
        Files.createDirectories(layout.state());

        String sourceHash = sha256(selfJar);
        Path manifest = layout.state().resolve("runtime.properties");
        Properties state = readProperties(manifest);
        Path runtimeMarker = layout.runtimeClasses().resolve(".lunararc-runtime.sha256");
        boolean current = sourceHash.equals(state.getProperty("core.sha256"))
                && Files.isRegularFile(layout.coreJar())
                && sourceHash.equals(safeSha256(layout.coreJar()))
                && Files.isDirectory(layout.runtimeClasses())
                && sourceHash.equals(readString(runtimeMarker))
                && Files.isRegularFile(layout.runtimeClasses().resolve("META-INF/neoforge.mods.toml"));

        if (!current) {
            System.out.println("[LunarArc] Preparing internal runtime under .lunararc...");
            atomicCopy(selfJar, layout.coreJar());
            rebuildRuntimeClasses(layout.coreJar(), layout.runtimeClasses());
            atomicWriteString(runtimeMarker, sourceHash);

            state.clear();
            state.setProperty("core.sha256", sourceHash);
            state.setProperty("minecraft", versions.getProperty("minecraft", "unknown"));
            state.setProperty("version", versions.getProperty("version", "unknown"));
            state.setProperty("platform", platform == null ? "unknown" : platform);
            state.setProperty("runtime.format", "2");
            atomicStore(state, manifest);
        }

        System.setProperty("lunararc.home", layout.root().toString());
        System.setProperty("lunararc.runtime", layout.runtime().toString());
        System.setProperty("lunararc.runtime.classes", layout.runtimeClasses().toString());
        System.setProperty("lunararc.pluginLibraries", layout.pluginLibraries().toString());
        System.setProperty("lunararc.mappings", layout.mappings().toString());
        System.setProperty("lunararc.cache", layout.cache().toString());
        return layout;
    }

    private static void rebuildRuntimeClasses(Path jarPath, Path target) throws IOException {
        if (Files.exists(target)) {
            try (var walk = Files.walk(target)) {
                walk.sorted((a, b) -> b.getNameCount() - a.getNameCount()).forEach(path -> {
                    try { Files.deleteIfExists(path); } catch (IOException e) { throw new RuntimeException(e); }
                });
            } catch (RuntimeException e) {
                if (e.getCause() instanceof IOException io) throw io;
                throw e;
            }
        }
        Files.createDirectories(target);
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            var entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory()) continue;
                String name = entry.getName();
                // Runtime classes/resources only. Embedded dependency jars stay jars.
                if (name.startsWith("META-INF/libraries/")) continue;
                Path out = target.resolve(name).normalize();
                if (!out.startsWith(target)) throw new IOException("Unsafe runtime entry: " + name);
                Files.createDirectories(out.getParent());
                try (InputStream in = jar.getInputStream(entry)) {
                    Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static Properties readProperties(Path path) {
        Properties p = new Properties();
        if (!Files.isRegularFile(path)) return p;
        try (InputStream in = Files.newInputStream(path)) { p.load(in); } catch (IOException ignored) {}
        return p;
    }

    private static void atomicCopy(Path source, Path target) throws IOException {
        Files.createDirectories(target.getParent());
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        Files.copy(source, tmp, StandardCopyOption.REPLACE_EXISTING);
        moveAtomically(tmp, target);
    }

    private static void atomicStore(Properties p, Path target) throws IOException {
        Files.createDirectories(target.getParent());
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        try (OutputStream out = Files.newOutputStream(tmp)) {
            p.store(out, "LunarArc internal runtime state - safe to delete");
        }
        moveAtomically(tmp, target);
    }

    private static void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }


    private static String safeSha256(Path path) {
        try { return sha256(path); } catch (Exception ignored) { return ""; }
    }

    private static String readString(Path path) {
        if (!Files.isRegularFile(path)) return "";
        try { return Files.readString(path).trim(); } catch (IOException ignored) { return ""; }
    }

    private static void atomicWriteString(Path target, String value) throws IOException {
        Files.createDirectories(target.getParent());
        Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
        Files.writeString(tmp, value + System.lineSeparator());
        moveAtomically(tmp, target);
    }

    private static String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(path)) {
            byte[] buffer = new byte[128 * 1024];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                if (read > 0) digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }
}
