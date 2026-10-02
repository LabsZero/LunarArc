package io.lunararcdevs.lunararc.launcher;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class QuiltInstaller {
    public static void install(Path workingDir, java.util.Properties versions, Path selfPath) throws Exception {
        Path quiltServerJar = workingDir.resolve("quilt-server-launch.jar");
        Path versionSentinel = workingDir.resolve(".lunararc-quilt-version");

        String mcVersion = LauncherUtils.requireVersion(versions, "minecraft");
        String loaderVersion = LauncherUtils.requireVersion(versions, "quilt");
        String installerVersion = LauncherUtils.requireVersion(versions, "quiltInstaller");

        Path minecraftServerJar = LauncherUtils.minecraftServerJar(workingDir, mcVersion);
        Path installerJar = Paths.get("quilt-" + mcVersion + "-" + loaderVersion + "-installer.jar");

        String installerUrl = String.format(
                "https://maven.quiltmc.org/repository/release/org/quiltmc/quilt-installer/%s/quilt-installer-%s.jar",
                installerVersion, installerVersion);

        String combinedVersion = loaderVersion + ":" + installerVersion;
        boolean needsInstall = !Files.exists(quiltServerJar) || !Files.exists(minecraftServerJar);

        if (!needsInstall && Files.exists(versionSentinel)) {
            String installedVersion = Files.readString(versionSentinel).trim();
            if (!installedVersion.equals(combinedVersion)) {
                needsInstall = true;
            }
        }

        if (!needsInstall) {
            String missingJar = LauncherUtils.missingManifestClassPathJar(quiltServerJar);
            if (missingJar != null) {
                ConsoleUI.printStep("install.quilt.reinstall_missing_jar", loaderVersion, missingJar);
                needsInstall = true;
            }
        }

        if (needsInstall) {
            ConsoleUI.printStep("install.downloading_libraries");
            if (!Files.exists(installerJar)) {
                Downloader.download(installerUrl, installerJar);
            }

            ProcessBuilder pb = new ProcessBuilder(
                    LauncherUtils.getJavaExecutable(), "-jar", installerJar.toAbsolutePath().toString(),
                    "install", "server", mcVersion, loaderVersion, "--download-server");
            pb.directory(workingDir.toFile());
            pb.inheritIO();
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                ConsoleUI.printError("install.quilt.failed_exit_code", exitCode);
                return;
            }

            Path installOutput = workingDir.resolve("server");
            if (Files.isDirectory(installOutput)) {
                LauncherUtils.adoptServerJar(installOutput, minecraftServerJar);
                LauncherUtils.mergeUp(installOutput, workingDir);
            }

            if (!Files.exists(quiltServerJar)) {
                ConsoleUI.printError("install.quilt.missing_output_jar", quiltServerJar.getFileName());
                return;
            }

            if (!Files.exists(minecraftServerJar)) {
                Path altJar = workingDir.resolve("minecraft_server." + mcVersion + ".jar");
                if (Files.exists(altJar)) {
                    Files.move(altJar, minecraftServerJar);
                }
            }

            Files.writeString(versionSentinel, combinedVersion);
        }

        QuiltLauncher.launch(workingDir, selfPath, minecraftServerJar);
    }
}
