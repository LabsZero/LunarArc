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
 *
 * <p>Paper recognises three values, not two. Besides {@code mojang} and {@code spigot} there is
 * {@code mojang+yarn}, which Paper's own remapper stamps onto every jar it rewrites - so a plugin
 * that has already been through a real Paper install, or a jar built by paperweight against
 * Mojang names with Yarn parameter names, arrives carrying that value. Both Mojang forms mean the
 * same thing to us: the jar's NMS references are already in the namespace the server runs in, so
 * leave them alone. Rejecting the third value made those plugins fail to load on a server that
 * could have run them unchanged.</p>
 */
public enum PluginMappingNamespace {
    MOJANG(false),
    SPIGOT(true);

    public static final String MANIFEST_ATTRIBUTE = "paperweight-mappings-namespace";

    private static final String MOJANG_NAMESPACE = "mojang";
    private static final String MOJANG_PLUS_YARN_NAMESPACE = "mojang+yarn";
    private static final String SPIGOT_NAMESPACE = "spigot";

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
            // mojang+yarn differs from mojang only in which parameter names the jar carries, which
            // nothing here reads. Both say the NMS references are already Mojang named.
            case MOJANG_NAMESPACE, MOJANG_PLUS_YARN_NAMESPACE -> MOJANG;
            case SPIGOT_NAMESPACE -> SPIGOT;
            // Paper refuses an unknown namespace rather than guessing at it, and so do we: a jar
            // naming a namespace we cannot remap from would be silently mangled by the wrong
            // mapping set, which is worse than not loading. The list is the whole set Paper knows.
            default -> throw new IllegalArgumentException("Unsupported " + MANIFEST_ATTRIBUTE
                    + " '" + value + "' in " + pluginFile.getName()
                    + "; LunarArc 1.21.1 supports '" + MOJANG_NAMESPACE + "', '"
                    + MOJANG_PLUS_YARN_NAMESPACE + "' or '" + SPIGOT_NAMESPACE + "'");
        };
    }
}
