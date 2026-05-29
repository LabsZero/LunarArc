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
        try {
            Properties versions = loadProperties("lunararc-launcher.properties");
            String minecraftVersion = versions.getProperty("minecraft", "unknown");
            String projectVersion = versions.getProperty("version", "unknown");
            String buildName = versions.getProperty("buildname", "unknown");
            
            ConsoleUI.printLogo(minecraftVersion);
            
            // Check for updates
            UpdateChecker.check(projectVersion, buildName);

            ConsoleUI.printStep("step.initializing");
            LibraryExtractor.extractLibraries();

            Path workingDir = Paths.get("").toAbsolutePath();
            Path lunararcDir = workingDir.resolve(".lunararc");
            if (!Files.exists(lunararcDir)) {
                ConsoleUI.printStep("step.creating_dir", lunararcDir.getFileName());
                Files.createDirectories(lunararcDir);
            }

            // Platform persistence logic
            Path configPath = workingDir.resolve("lunararc.conf");
            Properties config = new Properties();
            String choice = "";
            
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

            // Clean up any legacy bridge files we may have left in mods/ in older versions
            Path modsDir = Paths.get("mods");
            if (Files.exists(modsDir)) {
                Files.list(modsDir)
                        .filter(p -> p.getFileName().toString().toLowerCase().contains("lunararc"))
                        .forEach(p -> { try { Files.delete(p); } catch (Exception ignored) {} });
            }

            // Resolve the self-JAR path once; platform launchers use it for classpath injection
            Path selfPath = Paths.get(Launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                    .toAbsolutePath();

            switch (choice) {
                case "1":
                    ConsoleUI.printHeader("NeoForge Boot Sequence");
                    NeoForgeInstaller.install(lunararcDir, versions, selfPath);
                    break;
                case "2":
                    ConsoleUI.printHeader("Forge Boot Sequence");
                    ForgeInstaller.install(lunararcDir, versions, selfPath);
                    break;
                case "3":
                    ConsoleUI.printHeader("Fabric Boot Sequence");
                    FabricInstaller.install(lunararcDir, versions, selfPath);
                    break;
                case "4":
                    ConsoleUI.printHeader("Quilt Boot Sequence");
                    QuiltInstaller.install(lunararcDir, versions, selfPath);
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
