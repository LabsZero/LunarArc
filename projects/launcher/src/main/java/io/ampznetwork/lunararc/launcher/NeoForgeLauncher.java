package io.ampznetwork.lunararc.launcher;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

public class NeoForgeLauncher {

    public static void launch(Path workingDir, Path selfPath) throws Exception {
        Path libDir = Paths.get("libraries");
        if (!Files.exists(libDir)) {
            System.err.println("[LunarArc] Error: 'libraries' folder missing. Installation may have failed.");
            return;
        }

        // Deploy LunarArc to .lunararc/mods/ (hidden from users) instead of mods/
        Path bridgeModsDir = deployBridgeToHiddenDir(selfPath);

        // Tell FML to scan our hidden mods dir instead of (or in addition to) mods/
        if (bridgeModsDir != null) {
            String modsPath = bridgeModsDir.toAbsolutePath().toString();
            System.setProperty("fml.modsDir", modsPath);
            System.setProperty("fml.modFolder", modsPath);
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
            legacyLaunch(argsFile);
        }
    }

    private static Path deployBridgeToHiddenDir(Path selfPath) {
        try {
            Path modsDir = Paths.get(".lunararc", "mods");
            Files.createDirectories(modsDir);
            if (selfPath != null && Files.exists(selfPath)) {
                Path target = modsDir.resolve("lunararc-bridge.jar");
                Files.copy(selfPath, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                System.out.println("[LunarArc] Bridge deployed to " + target.toAbsolutePath());
            }
            return modsDir;
        } catch (Exception e) {
            System.err.println("[LunarArc] Warning: could not deploy bridge to .lunararc/mods/: " + e.getMessage());
            return null;
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
        System.out.println("[LunarArc] Launching NeoForge in-process (Arclight-style)...");

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

        if (mainClass == null) {
            System.err.println("[LunarArc] Could not determine NeoForge main class. Falling back to legacy launch.");
            legacyLaunch(argsFile);
            return;
        }

        gameArgs.add("--nogui");
        System.out.println("[LunarArc] Invoking NeoForge main: " + mainClass);
        try {
            Method main = Class.forName(mainClass, true, ClassLoader.getSystemClassLoader())
                    .getMethod("main", String[].class);
            main.invoke(null, (Object) gameArgs.toArray(new String[0]));
        } catch (ClassNotFoundException e) {
            System.err.println("[LunarArc] Same-JVM launch failed (" + e.getMessage() + "); falling back to child process.");
            legacyLaunch(argsFile);
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

    private static void legacyLaunch(Path argsFile) throws Exception {
        System.out.println("[LunarArc] Launching NeoForge via child process...");
        List<String> jvmArgs = Files.readAllLines(argsFile);
        List<String> command = new ArrayList<>();
        command.add(LauncherUtils.getJavaExecutable());

        for (String line : jvmArgs) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            for (String part : line.split(" ")) {
                if (!part.isEmpty()) command.add(part);
            }
        }
        command.add("--nogui");

        System.out.println("[LunarArc] Booting NeoForge Core...");
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.inheritIO();
        Process process = pb.start();
        System.exit(process.waitFor());
    }
}
