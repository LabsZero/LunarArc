package io.ampznetwork.lunararc.launcher;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ForgeLauncher {
    public static void launch(Path workingDir, Path selfPath) throws Exception {
        System.out.println("[LunarArc] Preparing Forge launch arguments...");

        Path libDir = Paths.get("libraries");
        Path argsFile = null;
        try (var stream = Files.walk(libDir)) {
            String preferred = System.getProperty("os.name", "").toLowerCase().contains("win")
                    ? "win_args.txt" : "unix_args.txt";
            argsFile = stream.filter(p -> p.getFileName().toString().equals(preferred))
                    .findFirst().orElse(null);
        }
        if (argsFile == null) {
            System.err.println("[LunarArc] Error: Could not find Forge args file.");
            return;
        }

        Path bridgeModsDir = Paths.get(".lunararc", "mods");
        Files.createDirectories(bridgeModsDir);
        if (selfPath != null && Files.exists(selfPath)) {
            Path target = bridgeModsDir.resolve("lunararc-bridge.jar");
            Files.copy(selfPath, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            System.out.println("[LunarArc] Bridge deployed to " + target.toAbsolutePath());
        }

        List<String> jvmArgs = Files.readAllLines(argsFile);
        List<String> command = new ArrayList<>();
        command.add(LauncherUtils.getJavaExecutable());

        // Both of these must precede the args file, whose last token is the main class: a -D
        // after the main class is a game argument, not a system property. fml.modsDir was being
        // added after it and so was never set.
        command.add("-Dfml.modsDir=" + bridgeModsDir.toAbsolutePath());
        List<String> inherited = LauncherUtils.inheritedJvmArguments("fml.modsDir");
        if (!inherited.isEmpty()) {
            System.out.println("[LunarArc] Passing JVM arguments through to the server: "
                    + String.join(" ", inherited));
        }
        command.addAll(inherited);

        for (String line : jvmArgs) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            for (String part : line.split(" ")) {
                if (!part.isEmpty()) command.add(part);
            }
        }

        command.add("--nogui");

        System.out.println("[LunarArc] Booting Forge...");
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.inheritIO();
        Process process = pb.start();
        System.exit(process.waitFor());
    }
}
