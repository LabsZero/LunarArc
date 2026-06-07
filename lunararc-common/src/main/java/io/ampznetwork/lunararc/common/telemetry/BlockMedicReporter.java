package io.ampznetwork.lunararc.common.telemetry;

import io.ampznetwork.lunararc.common.config.LunarArcConfig;
import io.ampznetwork.lunararc.common.server.LunarArcVersionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BlockMedicReporter {

    private static final Logger LOGGER = LoggerFactory.getLogger("LunarArc-BlockMedic");
    private static final String ENDPOINT = "https://blockmedic.ampznetwork.com/api/1/log";
    private static final long MAX_LOG_BYTES = 8 * 1024 * 1024; // 8 MB guard

    public static void uploadLog(String context) {
        if (!LunarArcConfig.isBlockMedicEnabled()) return;
        Thread.ofVirtual().name("lunararc-blockmedic").start(() -> {
            try {
                String content = readLatestLog();
                if (content == null || content.isEmpty()) return;

                String metadata = "["
                        + metaEntry("version", LunarArcVersionInfo.projectVersion(), "LunarArc Version", true) + ","
                        + metaEntry("minecraft", LunarArcVersionInfo.minecraftVersion(), "Minecraft Version", true) + ","
                        + metaEntry("context", context, "Context", true)
                        + "]";

                String payload = "{"
                        + "\"content\":" + jsonStr(content) + ","
                        + "\"source\":\"LunarArc\","
                        + "\"metadata\":" + metadata
                        + "}";

                String response = post(payload);
                String url = extract(response, "url");
                String errorCount = extract(response, "errors");
                LOGGER.info("[BlockMedic] Log uploaded ({} errors detected). View at: {}",
                        errorCount.isEmpty() ? "?" : errorCount, url);
            } catch (Exception e) {
                LOGGER.debug("[BlockMedic] Log upload failed: {}", e.getMessage());
            }
        });
    }

    private static String readLatestLog() {
        for (String candidate : new String[]{"logs/latest.log", "latest.log"}) {
            Path p = Paths.get(candidate);
            if (!Files.exists(p)) continue;
            try {
                long size = Files.size(p);
                if (size > MAX_LOG_BYTES) {
                    // Read the last MAX_LOG_BYTES so we don't OOM on huge logs
                    byte[] buf = new byte[(int) MAX_LOG_BYTES];
                    try (var in = Files.newInputStream(p)) {
                        long skip = size - MAX_LOG_BYTES;
                        long skipped = 0;
                        while (skipped < skip) skipped += in.skip(skip - skipped);
                        int read = 0, total = 0;
                        while (total < buf.length && (read = in.read(buf, total, buf.length - total)) != -1)
                            total += read;
                        return new String(buf, 0, total, StandardCharsets.UTF_8);
                    }
                }
                return Files.readString(p);
            } catch (Exception ignored) {}
        }
        return null;
    }

    private static String post(String json) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) URI.create(ENDPOINT).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("User-Agent", "LunarArc/" + LunarArcVersionInfo.projectVersion());
        conn.setDoOutput(true);
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(10_000);
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        conn.setRequestProperty("Content-Length", String.valueOf(body.length));
        try (OutputStream out = conn.getOutputStream()) {
            out.write(body);
        }
        try (InputStream in = conn.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String extract(String json, String key) {
        String search = "\"" + key + "\":";
        int idx = json.indexOf(search);
        if (idx < 0) return "";
        int start = idx + search.length();
        if (start >= json.length()) return "";
        char first = json.charAt(start);
        if (first == '"') {
            int end = json.indexOf('"', start + 1);
            return end > start ? json.substring(start + 1, end) : "";
        }
        int end = json.indexOf(',', start);
        if (end < 0) end = json.indexOf('}', start);
        return end > start ? json.substring(start, end).trim() : "";
    }

    private static String metaEntry(String key, String value, String label, boolean visible) {
        return "{\"key\":" + jsonStr(key) + ",\"value\":" + jsonStr(value)
                + ",\"label\":" + jsonStr(label) + ",\"visible\":" + visible + "}";
    }

    private static String jsonStr(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t") + "\"";
    }
}
