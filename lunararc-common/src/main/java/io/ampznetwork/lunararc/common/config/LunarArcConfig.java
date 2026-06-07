package io.ampznetwork.lunararc.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class LunarArcConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("LunarArc");
    private static final String CONFIG_FILE = "lunararc.yml";

    private static boolean velocityEnabled = false;
    private static byte[] velocitySecret = new byte[0];

    public static void load() {
        File file = new File(CONFIG_FILE);
        if (!file.exists()) {
            writeDefaults(file);
        }

        try (FileInputStream in = new FileInputStream(file)) {
            Yaml yaml = new Yaml();
            Map<String, Object> root = yaml.load(in);
            if (root == null) return;

            Object proxy = root.get("proxy");
            if (proxy instanceof Map<?, ?> proxyMap) {
                Object velocity = proxyMap.get("velocity");
                if (velocity instanceof Map<?, ?> velocityMap) {
                    Object enabled = velocityMap.get("enabled");
                    velocityEnabled = Boolean.TRUE.equals(enabled);
                    Object secret = velocityMap.get("secret");
                    if (secret instanceof String s && !s.isEmpty()) {
                        velocitySecret = s.getBytes(StandardCharsets.UTF_8);
                    }
                }
            }

            LOGGER.info("[LunarArc] Config loaded (velocity={}).", velocityEnabled);
        } catch (Exception e) {
            LOGGER.error("[LunarArc] Failed to load lunararc.yml — using defaults.", e);
        }
    }

    private static void writeDefaults(File file) {
        String defaults = """
                # LunarArc configuration
                proxy:
                  velocity:
                    # Set to true and provide the forwarding secret if using Velocity
                    enabled: false
                    secret: ""
                """;
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(defaults);
        } catch (IOException e) {
            LOGGER.error("[LunarArc] Could not write default lunararc.yml", e);
        }
    }

    public static boolean isVelocityEnabled() {
        return velocityEnabled;
    }

    public static byte[] getVelocitySecret() {
        return velocitySecret;
    }
}
