package io.ampznetwork.lunararc.launcher;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LauncherUtils {
    public static String requireVersion(java.util.Properties versions, String key) {
        String value = versions.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing launcher version property: " + key);
        }
        return value;
    }

    public static String getJavaExecutable() {
        String javaHome = System.getProperty("java.home");
        String os = System.getProperty("os.name").toLowerCase();
        boolean isWindows = os.contains("win");

        Path javaPath = Paths.get(javaHome, "bin", isWindows ? "java.exe" : "java");
        if (Files.exists(javaPath)) {
            return javaPath.toAbsolutePath().toString();
        }
        return "java";
    }

    /**
     * Deploys the LunarArc fat JAR into the {@code mods/} folder so the mod
     * loader discovers it as a real mod.
     *
     * FancyModLoader (NeoForge/Forge) only scans the {@code mods/} folder for
     * {@code neoforge.mods.toml} / {@code mods.toml} in a production
     * environment — entries on the legacy classpath are loaded as plain
     * libraries and never registered as mods, so their mixin configs are
     * never applied and the Bukkit/Paper bridge never initialises. Copying the
     * self-JAR here is the reliable, version-independent mechanism.
     */
    public static void deployBridge(Path selfPath) throws java.io.IOException {
        if (selfPath == null) {
            System.err.println("[LunarArc] Could not resolve self JAR path; bridge not deployed to mods/.");
            return;
        }
        Path modsDir = Paths.get("mods");
        Files.createDirectories(modsDir);
        Path target = modsDir.resolve("lunararc-bridge.jar");
        Files.copy(selfPath, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        System.out.println("[LunarArc] Deployed bridge mod to " + target.toAbsolutePath());
    }
}
