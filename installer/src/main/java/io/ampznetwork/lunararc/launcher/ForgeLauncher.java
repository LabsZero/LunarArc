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

        // Deploy to hidden .lunararc/mods/ (keeps user mods/ folder clean)
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

        for (String line : jvmArgs) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            for (String part : line.split(" ")) {
                if (!part.isEmpty()) command.add(part);
            }
        }

        command.add("-Dfml.modsDir=" + bridgeModsDir.toAbsolutePath());
        command.add("--nogui");

        System.out.println("[LunarArc] Booting Forge...");
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.inheritIO();
        Process process = pb.start();
        System.exit(process.waitFor());
    }
}
