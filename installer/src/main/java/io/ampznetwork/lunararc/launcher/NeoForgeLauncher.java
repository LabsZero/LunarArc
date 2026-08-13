package io.ampznetwork.lunararc.launcher;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;

public class NeoForgeLauncher {

    public static void launch(Path workingDir, Path selfPath) throws Exception {
        removeLegacyBridgeCopy();

        Path libDir = Paths.get("libraries");
        if (!Files.exists(libDir)) {
            System.err.println("[LunarArc] Error: 'libraries' folder missing. Installation may have failed.");
            return;
        }

        // Find args file
        Path argsFile = findArgsFile(libDir);
        if (argsFile == null) {
            System.err.println("[LunarArc] Error: Could not find NeoForge args file.");
            return;
        }

        if (LunarArcAgent.instrumentation != null) {
            sameJvmLaunch(selfPath, argsFile);
        } else {
            legacyLaunch(argsFile, selfPath);
        }
    }


    private static void removeLegacyBridgeCopy() {
        try {
            Path oldBridge = Paths.get("mods", "lunararc-bridge.jar");
            if (Files.deleteIfExists(oldBridge)) {
                System.out.println("[LunarArc] Removed legacy mods/lunararc-bridge.jar; LunarArc now boots from the loader bootstrap layer.");
            }
        } catch (Exception e) {
            System.err.println("[LunarArc] Warning: could not remove legacy mods/lunararc-bridge.jar: " + e.getMessage());
        }
    }

    private static Path findArgsFile(Path libDir) throws Exception {
        String preferred = System.getProperty("os.name", "").toLowerCase().contains("win")
                ? "win_args.txt" : "unix_args.txt";
        try (var stream = Files.walk(libDir)) {
            Path found = stream.filter(p -> p.getFileName().toString().equals(preferred))
                    .findFirst().orElse(null);
            if (found != null) return found;
        }
        // Fallback to any *_args.txt
        try (var stream = Files.walk(libDir)) {
            return stream.filter(p -> p.getFileName().toString().endsWith("_args.txt"))
                    .findFirst().orElse(null);
        }
    }

    private static void sameJvmLaunch(Path selfPath, Path argsFile) throws Exception {
        System.out.println("[LunarArc] Launching NeoForge in-process...");

        // Flatten all lines into one token list so index-based look-ahead works across lines.
        List<String> tokens = new ArrayList<>();
        for (String raw : Files.readAllLines(argsFile)) {
            for (String tok : raw.trim().split("\\s+")) {
                if (!tok.isEmpty() && !tok.startsWith("#")) tokens.add(tok);
            }
        }

        List<String> gameArgs = new ArrayList<>();
        String mainClass = null;
        int i = 0;
        while (i < tokens.size()) {
            String token = tokens.get(i++);

            if (token.startsWith("-D")) {
                String kv = token.substring(2);
                int eq = kv.indexOf('=');
                if (eq > 0) {
                    String key = kv.substring(0, eq);
                    if (!key.equals("fml.modsDir") && !key.equals("fml.modFolder"))
                        System.setProperty(key, kv.substring(eq + 1));
                } else {
                    System.setProperty(kv, "");
                }
            } else if (token.equals("-p") || token.equals("--module-path")
                    || token.equals("-cp") || token.equals("-classpath") || token.equals("--classpath")) {
                // Two-part flag: consume the next token as the path list.
                if (i < tokens.size()) addPathEntriesToClassLoader(tokens.get(i++));
            } else if (token.startsWith("--module-path=")) {
                addPathEntriesToClassLoader(token.substring("--module-path=".length()));
            } else if (token.startsWith("--classpath=") || token.startsWith("-classpath=")) {
                addPathEntriesToClassLoader(token.substring(token.indexOf('=') + 1));
            } else if (token.equals("--add-opens") || token.equals("--add-exports")) {
                // Apply via Instrumentation.redefineModule so they take effect in-process.
                if (i < tokens.size()) applyModuleDirective(token, tokens.get(i++));
            } else if (token.startsWith("--add-opens=")) {
                applyModuleDirective("--add-opens", token.substring("--add-opens=".length()));
            } else if (token.startsWith("--add-exports=")) {
                applyModuleDirective("--add-exports", token.substring("--add-exports=".length()));
            } else if (token.startsWith("-X") || token.startsWith("-ea") || token.startsWith("-da")
                    || token.startsWith("--add-") || token.startsWith("-javaagent")) {
                // Other JVM-only args that cannot be applied post-startup — skip.
            } else if (token.contains(File.separator) && (token.endsWith(".jar") || token.endsWith(".zip"))) {
                // Bare JAR path (uncommon but possible in some args files).
                addJarToClassLoader(Paths.get(token));
            } else if (token.matches("[a-zA-Z][\\w.]+\\.[A-Z][\\w]*") && mainClass == null) {
                mainClass = token;
            } else {
                gameArgs.add(token);
            }
        }

        // Always inject the LunarArc JAR itself so FML's ServiceLoader sees LunarArcModLocator.
        if (selfPath != null && Files.exists(selfPath)) {
            LunarArcAgent.instrumentation.appendToSystemClassLoaderSearch(new JarFile(selfPath.toFile()));
        }

        if (fmlClassesAvailable()) {
            System.out.println("[LunarArc] LunarArcModLocator available in bootstrap layer.");
        }

        if (mainClass == null) {
            System.err.println("[LunarArc] Could not determine NeoForge main class. Falling back to child-process launch.");
            legacyLaunch(argsFile, selfPath);
            return;
        }

        gameArgs.add("--nogui");
        System.out.println("[LunarArc] Invoking NeoForge main: " + mainClass);
        try {
            Method main = Class.forName(mainClass, true, ClassLoader.getSystemClassLoader())
                    .getMethod("main", String[].class);
            main.invoke(null, (Object) gameArgs.toArray(new String[0]));
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            System.err.println("[LunarArc] Same-JVM launch failed (" + cause.getClass().getSimpleName()
                    + ": " + cause.getMessage() + "); falling back to child process.");
            legacyLaunch(argsFile, selfPath);
        }
    }

    /**
     * Returns true if FML internal classes are available on the system classloader,
     * meaning LunarArcModLocator.scanMods() should be able to self-register via reflection.
     */
    private static boolean fmlClassesAvailable() {
        try {
            Class.forName("cpw.mods.jarhandling.SecureJar", false, ClassLoader.getSystemClassLoader());
            Class.forName("net.neoforged.fml.loading.moddiscovery.ModFile", false, ClassLoader.getSystemClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static void applyModuleDirective(String directive, String spec) {
        // spec = "moduleName/packageName=target" — target is ignored because the JARs
        // are loaded into the unnamed module (not as named modules), so we open/export
        // to the unnamed module unconditionally.
        try {
            int slash = spec.indexOf('/');
            if (slash < 0) return;
            String moduleName = spec.substring(0, slash);
            String rest = spec.substring(slash + 1);
            String packageName = rest.contains("=") ? rest.substring(0, rest.indexOf('=')) : rest;

            Module module = ModuleLayer.boot().findModule(moduleName).orElse(null);
            if (module == null) return;

            Module unnamed = ClassLoader.getSystemClassLoader().getUnnamedModule();
            boolean isOpens = "--add-opens".equals(directive);
            LunarArcAgent.instrumentation.redefineModule(
                    module,
                    Set.of(),
                    isOpens ? Map.of() : Map.of(packageName, Set.of(unnamed)),
                    isOpens ? Map.of(packageName, Set.of(unnamed)) : Map.of(),
                    Set.of(),
                    Map.of()
            );
        } catch (Exception e) {
            System.err.println("[LunarArc] Warning: could not apply " + directive + " " + spec + ": " + e.getMessage());
        }
    }

    private static void addPathEntriesToClassLoader(String pathList) {
        for (String entry : pathList.split(File.pathSeparator)) {
            if (!entry.isEmpty()) addJarToClassLoader(Paths.get(entry));
        }
    }

    private static void addJarToClassLoader(Path jar) {
        String name = jar.toString();
        if (!name.endsWith(".jar") && !name.endsWith(".zip")) return;
        if (!Files.exists(jar)) return;
        try {
            LunarArcAgent.instrumentation.appendToSystemClassLoaderSearch(new JarFile(jar.toFile()));
        } catch (Exception e) {
            System.err.println("[LunarArc] Warning: could not inject " + jar.getFileName() + ": " + e.getMessage());
        }
    }

    private static void legacyLaunch(Path argsFile, Path selfPath) throws Exception {
        System.out.println("[LunarArc] Launching NeoForge via child process...");
        List<String> tokens = new ArrayList<>();
        for (String line : Files.readAllLines(argsFile)) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            for (String part : line.split("\\s+")) {
                if (!part.isEmpty()) tokens.add(part);
            }
        }

        List<String> command = new ArrayList<>();
        command.add(LauncherUtils.getJavaExecutable());
        Path lunararcHome = Paths.get(System.getProperty("lunararc.home", ".lunararc")).toAbsolutePath().normalize();
        Path runtimeClasses = Paths.get(System.getProperty("lunararc.runtime.classes", lunararcHome.resolve("runtime/classes").toString()));
        command.add("-Dlunararc.home=" + lunararcHome);
        command.add("-Dlunararc.runtime.classes=" + runtimeClasses.toAbsolutePath());
        // Do not attach the LunarArc fat JAR as a javaagent in the child process.
        // The child uses NeoForge's normal mod classloader via the staged runtime JAR below.
        // Supplying the same classes as both a javaagent/system-classloader entry and a mod
        // gives Mixin two class identities for LunarArc classes and can abort during prepare().
        for (String token : tokens) {
            // Keep NeoForge's generated module/class path exactly as installed.
            // LunarArc is supplied separately through the prepared runtime below.
            command.add(token);
        }
        command.add("--nogui");

        // Production FML does not reliably discover MOD_CLASSES entries. The previous
        // child-process path therefore launched a perfectly healthy NeoForge server, but
        // LunarArc itself never appeared in FML's mod list. Without the LunarArc mod the
        // common MinecraftServerMixin is never applied, so CraftServer is never created
        // and the Bukkit/Paper plugin lifecycle never starts.
        //
        // Stage the already-prepared internal core JAR as a transient hidden mod for the
        // lifetime of the child process. This uses NeoForge's normal production discovery
        // path while keeping the user's actual mod JARs untouched.
        Path stagedRuntimeMod = stageRuntimeMod(lunararcHome, selfPath);
        if (stagedRuntimeMod != null) {
            System.out.println("[LunarArc] Staged internal runtime for NeoForge discovery: " + stagedRuntimeMod);
        } else {
            System.err.println("[LunarArc] Warning: could not stage internal NeoForge runtime; Bukkit plugins will not load.");
        }

        System.out.println("[LunarArc] Booting NeoForge Core...");
        ProcessBuilder pb = new ProcessBuilder(command);
        // Production child launches must expose LunarArc exactly once. MOD_CLASSES is a
        // development/userdev mechanism and is intentionally not set here; the staged JAR
        // is the sole LunarArc runtime source for FML and Mixin.
        pb.environment().remove("MOD_CLASSES");
        pb.inheritIO();
        Process process = pb.start();
        int exitCode = process.waitFor();
        cleanupRuntimeMod(stagedRuntimeMod);
        System.exit(exitCode);
    }
    private static Path stageRuntimeMod(Path lunararcHome, Path selfPath) {
        try {
            Path coreJar = lunararcHome.resolve("runtime/lunararc-core.jar").toAbsolutePath().normalize();
            Path source = Files.isRegularFile(coreJar) ? coreJar
                    : (selfPath != null && Files.isRegularFile(selfPath) ? selfPath.toAbsolutePath().normalize() : null);
            if (source == null) return null;

            Path modsDir = Paths.get("mods").toAbsolutePath().normalize();
            Files.createDirectories(modsDir);
            Path staged = modsDir.resolve(".lunararc-runtime.jar");
            Files.copy(source, staged, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            staged.toFile().deleteOnExit();
            return staged;
        } catch (Exception e) {
            System.err.println("[LunarArc] Failed to stage internal runtime mod: " + e);
            return null;
        }
    }

    private static void cleanupRuntimeMod(Path stagedRuntimeMod) {
        if (stagedRuntimeMod == null) return;
        try {
            Files.deleteIfExists(stagedRuntimeMod);
        } catch (Exception e) {
            System.err.println("[LunarArc] Warning: could not remove staged runtime mod "
                    + stagedRuntimeMod + ": " + e.getMessage());
        }
    }

}
