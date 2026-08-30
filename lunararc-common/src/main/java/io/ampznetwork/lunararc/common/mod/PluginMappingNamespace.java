package io.ampznetwork.lunararc.common.mod;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Mapping namespace used by a plugin's NMS references.
 *
 * <p>LunarArc follows Paper's 1.20.5+ loading model for Minecraft 1.21.1:
 * Bukkit/Spigot plugins default to Spigot mappings, while Paper plugins default
 * to Mojang mappings. The paperweight manifest attribute overrides that default.</p>
 */
public enum PluginMappingNamespace {
    MOJANG(false),
    SPIGOT(true);

    public static final String MANIFEST_ATTRIBUTE = "paperweight-mappings-namespace";

    private final boolean requiresNmsRemap;

    PluginMappingNamespace(boolean requiresNmsRemap) {
        this.requiresNmsRemap = requiresNmsRemap;
    }

    public boolean requiresNmsRemap() {
        return requiresNmsRemap;
    }

    public static PluginMappingNamespace detect(File pluginFile) {
        try (JarFile jar = new JarFile(pluginFile)) {
            Manifest manifest = jar.getManifest();
            if (manifest != null) {
                String declared = manifest.getMainAttributes().getValue(MANIFEST_ATTRIBUTE);
                if (declared != null && !declared.isBlank()) {
                    return parseDeclared(pluginFile, declared);
                }
            }

            // Paper 1.20.5+ default: Paper plugins are Mojang mapped; ordinary
            // Bukkit/Spigot plugins are treated as Spigot mapped.
            return jar.getJarEntry("paper-plugin.yml") != null ? MOJANG : SPIGOT;
        } catch (IOException error) {
            throw new IllegalStateException("Could not inspect plugin mappings namespace for "
                    + pluginFile.getName(), error);
        }
    }

    private static PluginMappingNamespace parseDeclared(File pluginFile, String value) {
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "mojang" -> MOJANG;
            case "spigot" -> SPIGOT;
            default -> throw new IllegalArgumentException("Unsupported " + MANIFEST_ATTRIBUTE
                    + " '" + value + "' in " + pluginFile.getName()
                    + "; LunarArc 1.21.1 supports only 'mojang' or 'spigot'");
        };
    }
}
