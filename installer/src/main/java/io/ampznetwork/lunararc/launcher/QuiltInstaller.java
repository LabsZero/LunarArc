package io.ampznetwork.lunararc.launcher;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class QuiltInstaller {
    public static void install(Path workingDir, java.util.Properties versions, Path selfPath) throws Exception {
        System.out.println("Installing Quilt...");
        Path quiltServerJar = workingDir.resolve("quilt-server-launch.jar");
        Path minecraftServerJar = workingDir.resolve("server.jar");
        Path versionSentinel = workingDir.resolve(".lunararc-quilt-version");

        String mcVersion = LauncherUtils.requireVersion(versions, "minecraft");
        String loaderVersion = LauncherUtils.requireVersion(versions, "quilt");
        String installerVersion = LauncherUtils.requireVersion(versions, "quiltInstaller");

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

        if (needsInstall) {
            System.out.println("Quilt or Minecraft server JAR missing. Starting installation...");
            if (!Files.exists(installerJar)) {
                Downloader.download(installerUrl, installerJar);
            }

            System.out.println("Running Quilt installer for version " + mcVersion
                    + " (Loader: " + loaderVersion + ")...");
            // The Quilt installer takes the loader version as a positional argument after the
            // Minecraft version, and spells the server download --download-server. It was being
            // called with "--loader-version <ver> --download-minecraft", neither of which it
            // accepts: it printed its usage text, downloaded nothing, and exited 0.
            ProcessBuilder pb = new ProcessBuilder(
                    LauncherUtils.getJavaExecutable(), "-jar", installerJar.toAbsolutePath().toString(),
                    "install", "server", mcVersion, loaderVersion, "--download-server");
            pb.inheritIO();
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                System.err.println("Quilt installer failed with exit code: " + exitCode);
                return;
            }

            // Exit code 0 is not evidence the install happened. The installer answers a command
            // line it does not understand by printing usage and exiting successfully, which is how
            // a wrong argument list turned into "Unable to access jarfile quilt-server-launch.jar"
            // several steps later, naming a file rather than the reason it was never made.
            if (!Files.exists(quiltServerJar)) {
                System.err.println("Quilt installer reported success but did not produce "
                        + quiltServerJar.getFileName() + ". The output above is from the installer;"
                        + " if it printed its usage text then the arguments it was given are wrong.");
                return;
            }

            if (!Files.exists(minecraftServerJar)) {
                Path altJar = workingDir.resolve("minecraft_server." + mcVersion + ".jar");
                if (Files.exists(altJar)) {
                    Files.move(altJar, minecraftServerJar);
                }
            }

            System.out.println("Quilt installation complete!");
            Files.writeString(versionSentinel, combinedVersion);
        }

        System.out.println("Quilt ready.");
        QuiltLauncher.launch(workingDir, selfPath);
    }
}
