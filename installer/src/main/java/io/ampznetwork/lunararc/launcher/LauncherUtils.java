package io.ampznetwork.lunararc.launcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

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

    /**
     * The loader's own {@code *_args.txt}, looked for under the loader's own path first.
     *
     * <p>{@code libraries/} is shared between every loader installed into the same server
     * directory, so a server that has run both NeoForge and Forge has an args file for each. The
     * search used to walk the whole tree and take the first match, which meant whichever the
     * filesystem happened to return - so Forge could boot with NeoForge's arguments, or the other
     * way round, depending on nothing more than directory order.</p>
     *
     * <p>The loader's own subtree is searched first. The whole-tree walk is kept only as a fallback
     * for a layout that does not match, and it is the caller's job to decide whether finding
     * nothing is an error.</p>
     */
    static Path findArgsFile(Path librariesDir, String loaderPath) throws IOException {
        return findArgsFile(librariesDir, loaderPath, true);
    }

    /**
     * As above, with control over the fallback.
     *
     * <p>Pass {@code false} to answer strictly about {@code loaderPath} - a caller deciding whether
     * a particular loader's install is still intact must not be satisfied by another loader's args
     * file, which is exactly what the whole-tree fallback would hand it.</p>
     */
    static Path findArgsFile(Path librariesDir, String loaderPath, boolean fallBackToWholeTree)
            throws IOException {
        if (!Files.isDirectory(librariesDir)) return null;
        String preferred = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")
                ? "win_args.txt" : "unix_args.txt";

        Path scoped = librariesDir.resolve(loaderPath);
        if (Files.isDirectory(scoped)) {
            Path found = firstMatch(scoped, preferred);
            if (found == null) found = firstMatch(scoped, null);
            if (found != null) return found;
        }
        if (!fallBackToWholeTree) return null;
        Path found = firstMatch(librariesDir, preferred);
        return found != null ? found : firstMatch(librariesDir, null);
    }

    /**
     * Every token an args file contributes to a launch command, comments and blank lines dropped.
     */
    static java.util.List<String> readArgsFileTokens(Path argsFile) throws IOException {
        java.util.List<String> tokens = new java.util.ArrayList<>();
        for (String line : Files.readAllLines(argsFile)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
            for (String part : trimmed.split(" ")) {
                if (!part.isEmpty()) tokens.add(part);
            }
        }
        return tokens;
    }

    private static Path firstMatch(Path root, String exactName) throws IOException {
        try (java.util.stream.Stream<Path> stream = Files.walk(root)) {
            return stream
                    .filter(path -> exactName == null
                            ? path.getFileName().toString().endsWith("_args.txt")
                            : path.getFileName().toString().equals(exactName))
                    .findFirst()
                    .orElse(null);
        }
    }

    /**
     * The jar a {@code -jar} in this command names but which is not on disk, or null if all resolve.
     *
     * <p>A loader's args file can name a jar of its own, and that jar sits in the server directory
     * rather than beside the args file. When it is missing the JVM says "Unable to access jarfile",
     * which names the file and not the reason - and the reason is usually that the install never
     * ran or did not finish. Checking first lets the caller say that instead.</p>
     */
    static String missingLaunchJar(java.util.List<String> command) {
        for (int i = 0; i < command.size() - 1; i++) {
            if (!"-jar".equals(command.get(i))) continue;
            String jar = command.get(i + 1);
            if (!Files.isRegularFile(Path.of(jar))) return jar;
        }
        return null;
    }
}
