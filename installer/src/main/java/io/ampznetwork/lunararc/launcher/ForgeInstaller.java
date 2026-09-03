package io.ampznetwork.lunararc.launcher;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ForgeInstaller {
    public static void install(Path workingDir, java.util.Properties versions, Path selfPath) throws Exception {
        System.out.println("Installing Forge...");

        String mcVersion = LauncherUtils.requireVersion(versions, "minecraft");
        String forgeVersion = LauncherUtils.requireVersion(versions, "forge");
        Path installerJar = Paths.get("forge-" + mcVersion + "-" + forgeVersion + "-installer.jar");
        String url = String.format(
                "https://maven.minecraftforge.net/net/minecraftforge/forge/%s-%s/forge-%s-%s-installer.jar", mcVersion,
                forgeVersion, mcVersion, forgeVersion);

        if (!Files.exists(installerJar)) {
            Downloader.download(url, installerJar);
        }

        Path libDir = Paths.get("libraries");
        Path versionSentinel = libDir.resolve(".lunararc-forge-version");
        boolean needsInstall = true;

        if (Files.exists(versionSentinel)) {
            String installedVersion = Files.readString(versionSentinel).trim();
            if (installedVersion.equals(forgeVersion) && installIntact(libDir, mcVersion, forgeVersion)) {
                needsInstall = false;
            }
        }

        if (needsInstall) {
            System.out.println("Running Forge installer (this may take a few minutes)...");

            ProcessBuilder pb = new ProcessBuilder(
                    LauncherUtils.getJavaExecutable(), "-jar", installerJar.toAbsolutePath().toString(), "--installServer");
            pb.inheritIO();
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                System.err.print("Forge installer failed with exit code: ");
                System.err.println(exitCode);
                return;
            }
            System.out.println("Forge installation complete!");
            Files.writeString(versionSentinel, forgeVersion);
        }

        System.out.println("Forge libraries ready.");
        ForgeLauncher.launch(workingDir, selfPath);
    }

    /**
     * Whether this Forge version's install is still on disk, rather than merely recorded.
     *
     * <p>The sentinel says which version was installed and nothing about whether it survived. Forge
     * 1.21.1's launch arguments name a shim jar that lives in the server directory rather than under
     * {@code libraries}, and a directory that has lost it - a run that was interrupted, or files
     * moved between directories - reached the launch anyway and died there with "Unable to access
     * jarfile forge-1.21.1-52.1.16-shim.jar", having skipped the install that would have put it
     * back. The Fabric and Quilt installers already check for their own output before trusting
     * their sentinels; this is the same check, done against what Forge's own args file asks for.</p>
     *
     * <p>The lookup is deliberately pinned to this exact version's directory and does not fall back
     * to a whole-tree search: {@code libraries} is shared with any other loader installed into the
     * same server directory, and NeoForge's args file must not be mistaken for evidence that Forge
     * is installed.</p>
     */
    private static boolean installIntact(Path libDir, String mcVersion, String forgeVersion) throws Exception {
        Path argsFile = LauncherUtils.findArgsFile(
                libDir, "net/minecraftforge/forge/" + mcVersion + "-" + forgeVersion, false);
        if (argsFile == null) {
            System.out.println("Forge " + forgeVersion + " is recorded as installed, but its launch "
                    + "arguments are missing. Installing again...");
            return false;
        }

        String missingJar = LauncherUtils.missingLaunchJar(LauncherUtils.readArgsFileTokens(argsFile));
        if (missingJar != null) {
            System.out.println("Forge " + forgeVersion + " is recorded as installed, but " + missingJar
                    + " is missing. Installing again...");
            return false;
        }
        return true;
    }
}
