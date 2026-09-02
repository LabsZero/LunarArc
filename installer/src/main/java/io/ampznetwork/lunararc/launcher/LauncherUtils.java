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
     * The JVM arguments this process was started with, filtered to the ones a spawned server JVM
     * should inherit.
     *
     * <p>Every launcher that starts the server as a child process built its command line from
     * scratch, so nothing the operator put on the original command line reached the JVM that
     * actually runs Minecraft. Heap settings are the visible casualty: a server started with
     * -Xmx4G ran the game on the JVM default of a quarter of system RAM, and its crash reports
     * said "JVM Flags: 0 total" while the operator was looking at the flags they had set. The
     * same silence swallowed -XX GC tuning and every -D property.</p>
     *
     * <p>What is deliberately not inherited:</p>
     * <ul>
     *   <li>-javaagent / -agentpath / -agentlib. The launcher jar is itself the agent on the
     *       in-process path; handing that to the child would start a second launcher inside the
     *       server it just started.</li>
     *   <li>Module-system flags (-p, --module-path, --add-opens, --add-exports, --add-modules).
     *       The loader's own args file already specifies exactly what the server needs, and the
     *       parent's copy is for running the launcher, not the game.</li>
     *   <li>Any -D whose key the caller sets itself, so an explicit value is never shadowed by an
     *       inherited duplicate.</li>
     * </ul>
     *
     * <p>Note for anyone reading a memory problem: on this path the parent JVM has already
     * reserved whatever heap it was given before it spawns anything, so an -Xms with
     * AlwaysPreTouch is committed twice over - once by the launcher that only waits, once by the
     * server. The in-process path, where the loader boots inside this JVM instead of a child, has
     * no such split and is the better answer where it is available.</p>
     *
     * @param ownProperties {@code -D} keys the caller sets explicitly, which must not be inherited
     */
    public static java.util.List<String> inheritedJvmArguments(String... ownProperties) {
        java.util.Set<String> owned = new java.util.HashSet<>(java.util.Arrays.asList(ownProperties));
        java.util.List<String> inherited = new java.util.ArrayList<>();
        java.util.List<String> arguments;
        try {
            arguments = java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments();
        } catch (Throwable unavailable) {
            // No management bean (a stripped or restricted runtime). Better to launch with the
            // defaults than not at all.
            return inherited;
        }

        for (String argument : arguments) {
            if (argument == null || argument.isBlank()) continue;
            if (argument.startsWith("-javaagent") || argument.startsWith("-agentpath")
                    || argument.startsWith("-agentlib")) {
                continue;
            }
            if (argument.equals("-p") || argument.startsWith("--module-path")
                    || argument.startsWith("--add-opens") || argument.startsWith("--add-exports")
                    || argument.startsWith("--add-modules") || argument.startsWith("--patch-module")) {
                continue;
            }
            if (argument.startsWith("-D")) {
                String body = argument.substring(2);
                int equals = body.indexOf('=');
                String key = equals > 0 ? body.substring(0, equals) : body;
                if (owned.contains(key)) continue;
            } else if (!argument.startsWith("-X")) {
                // Anything else is a launcher-specific or unrecognised flag; passing it on is more
                // likely to stop the server booting than to help.
                continue;
            }
            inherited.add(argument);
        }
        return inherited;
    }

}
