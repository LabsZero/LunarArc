package io.ampznetwork.lunararc.launcher;

import io.ampznetwork.lunararc.i18n.TranslationManager;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Scanner;

/**
 * The high-performance LunarArc Unified Launcher.
 * Designed to be better than Arclight by providing a cleaner, faster boot
 * sequence.
 */
public class Launcher {
    public static void main(String[] args) {
        // Arclight-style agent bootstrap: re-launch with -javaagent:self so premain()
        // gives us Instrumentation, allowing same-JVM mod loader injection without mods/.
        if (LunarArcAgent.instrumentation == null) {
            try {
                Path self = Paths.get(Launcher.class.getProtectionDomain()
                        .getCodeSource().getLocation().toURI()).toAbsolutePath();
                if (java.nio.file.Files.isRegularFile(self)) {
                    java.util.List<String> cmd = new java.util.ArrayList<>();
                    cmd.add(LauncherUtils.getJavaExecutable());
                    cmd.add("-javaagent:" + self);
                    cmd.add("-jar");
                    cmd.add(self.toString());
                    java.util.Collections.addAll(cmd, args);
                    ProcessBuilder pb = new ProcessBuilder(cmd);
                    pb.inheritIO();
                    System.exit(pb.start().waitFor());
                }
            } catch (Exception e) {
                // Fall through: agent unavailable, continue without Instrumentation
            }
        }

        try {
            Properties versions = loadProperties("lunararc-launcher.properties");
            String minecraftVersion = versions.getProperty("minecraft", "unknown");
            String projectVersion = versions.getProperty("version", "unknown");
            String buildName = versions.getProperty("buildName", "unknown");
            
            ConsoleUI.printLogo(minecraftVersion);

            // EULA check — must agree before server starts
            checkEula();

            // Check for updates
            UpdateChecker.check(projectVersion, buildName);

            ConsoleUI.printStep("step.initializing");
            LibraryExtractor.extractLibraries();

            Path workingDir = Paths.get("").toAbsolutePath();

            // Platform selection. Each platform-specific jar bakes a
            // "LunarArc-Platform" manifest attribute, so when the user runs e.g.
            // the NeoForge jar we launch it directly without prompting. Only a
            // universal/unmarked jar falls back to the persisted/prompted choice.
            String choice = platformChoiceFromManifest();

            if (choice == null || choice.isEmpty()) {
                Path configPath = workingDir.resolve("lunararc.conf");
                Properties config = new Properties();

                if (Files.exists(configPath)) {
                    try (InputStream in = Files.newInputStream(configPath)) {
                        config.load(in);
                        choice = config.getProperty("platform", "");
                    }
                }

                if (choice == null || choice.isEmpty()) {
                    System.out.println(TranslationManager.get("platform.select_header"));
                    System.out.println(TranslationManager.get("platform.neoforge", minecraftVersion));
                    System.out.println(TranslationManager.get("platform.forge", minecraftVersion));
                    System.out.println(TranslationManager.get("platform.fabric", minecraftVersion));
                    System.out.println(TranslationManager.get("platform.quilt", minecraftVersion));
                    System.out.println();
                    System.out.print(TranslationManager.get("platform.select_prompt"));

                    Scanner scanner = new Scanner(System.in);
                    choice = scanner.nextLine();

                    config.setProperty("platform", choice);
                    try (java.io.OutputStream out = Files.newOutputStream(configPath)) {
                        config.store(out, "LunarArc Server Configuration");
                    }
                } else {
                    ConsoleUI.printStep("step.auto_selecting", choice, configPath.getFileName());
                }
            }

            // Resolve the self-JAR path once; platform launchers use it for classpath injection
            Path selfPath = Paths.get(Launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                    .toAbsolutePath();

            String platformName = switch (choice) {
                case "1" -> "neoforge";
                case "2" -> "forge";
                case "3" -> "fabric";
                case "4" -> "quilt";
                default -> "unknown";
            };
            LunarArcRuntime.Layout runtime = LunarArcRuntime.prepare(workingDir, selfPath, versions, platformName);

            switch (choice) {
                case "1":
                    ConsoleUI.printHeader("NeoForge Boot Sequence");
                    NeoForgeInstaller.install(workingDir, versions, runtime.coreJar());
                    break;
                case "2":
                    ConsoleUI.printHeader("Forge Boot Sequence");
                    ForgeInstaller.install(workingDir, versions, runtime.coreJar());
                    break;
                case "3":
                    ConsoleUI.printHeader("Fabric Boot Sequence");
                    FabricInstaller.install(workingDir, versions, runtime.coreJar());
                    break;
                case "4":
                    ConsoleUI.printHeader("Quilt Boot Sequence");
                    QuiltInstaller.install(workingDir, versions, runtime.coreJar());
                    break;
                default:
                    ConsoleUI.printError("error.invalid_selection");
                    break;
            }

        } catch (Exception e) {
            ConsoleUI.printError("error.critical_failure");
            e.printStackTrace();
        }
    }

    /**
     * Resolves the boot platform from this jar's {@code LunarArc-Platform}
     * manifest attribute, mapping it to the numeric menu choice. Returns
     * {@code null} for a universal/unmarked jar so the caller falls back to the
     * persisted/prompted selection.
     */
    private static String platformChoiceFromManifest() {
        try {
            Path self = Paths.get(
                    Launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (!Files.isRegularFile(self)) {
                return null;
            }
            try (java.util.jar.JarFile jar = new java.util.jar.JarFile(self.toFile())) {
                java.util.jar.Manifest manifest = jar.getManifest();
                if (manifest == null) {
                    return null;
                }
                String platform = manifest.getMainAttributes().getValue("LunarArc-Platform");
                if (platform == null) {
                    return null;
                }
                switch (platform.trim().toLowerCase()) {
                    case "neoforge": return "1";
                    case "forge":    return "2";
                    case "fabric":   return "3";
                    case "quilt":    return "4";
                    default:         return null;
                }
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void checkEula() throws Exception {
        Path eulaFile = Paths.get("eula.txt");
        if (Files.exists(eulaFile)) {
            Properties eula = new Properties();
            try (InputStream in = Files.newInputStream(eulaFile)) {
                eula.load(in);
            }
            if ("true".equalsIgnoreCase(eula.getProperty("eula", "false"))) {
                return;
            }
        }
        System.out.println(TranslationManager.get("eula.header"));
        System.out.println(TranslationManager.get("eula.url"));
        System.out.println(TranslationManager.get("eula.prompt"));
        Scanner scanner = new Scanner(System.in);
        String response = scanner.nextLine().trim().toLowerCase();
        if (response.equals("yes") || response.equals("y") || response.equals("true")) {
            Properties eula = new Properties();
            eula.setProperty("eula", "true");
            try (java.io.OutputStream out = Files.newOutputStream(eulaFile)) {
                eula.store(out, "By changing the setting below to TRUE you are indicating your agreement to the EULA (https://aka.ms/MinecraftEULA).");
            }
            System.out.println(TranslationManager.get("eula.accepted"));
        } else {
            System.out.println(TranslationManager.get("eula.declined"));
            System.exit(0);
        }
    }

    private static Properties loadProperties(String name) {
        Properties props = new Properties();
        try (InputStream in = Launcher.class.getClassLoader().getResourceAsStream(name)) {
            if (in != null)
                props.load(in);
        } catch (Exception ignored) {
        }
        return props;
    }
}
