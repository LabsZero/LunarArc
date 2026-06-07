package io.ampznetwork.lunararc.common.telemetry;

import io.ampznetwork.lunararc.common.config.LunarArcConfig;
import io.ampznetwork.lunararc.common.server.LunarArcVersionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;

public class BlockMedicReporter {

    private static final Logger LOGGER = LoggerFactory.getLogger("LunarArc-BlockMedic");
    private static final String ENDPOINT = "https://blockmedic.ampznetwork.com/api/1/log";
    private static final long MAX_LOG_BYTES = 100L * 1024 * 1024; // 100 MB (BlockMedic limit)

    /** Modification time of the log that was most recently uploaded (0 = never uploaded). */
    private static volatile long lastUploadedModified = 0;

    /** Epoch-millis of the last successful upload — enforces BlockMedic's 60 req/min limit. */
    private static volatile long lastUploadTime = 0;

    /** Minimum millis between any two uploads (2 s keeps well within 60 req/min). */
    private static final long MIN_UPLOAD_INTERVAL_MS = 2_000;

    /** Rolling console-capture buffer — populated when no log file exists yet. */
    private static final java.util.Deque<String> consoleBuffer = new java.util.concurrent.ConcurrentLinkedDeque<>();
    private static volatile boolean consoleCapturing = false;

    /**
     * Installs a tee on System.out / System.err so we have a fallback copy of console
     * output for upload even before logs/latest.log is created.
     * Safe to call multiple times — installs at most once.
     */
    public static synchronized void startConsoleCapture() {
        if (consoleCapturing) return;
        consoleCapturing = true;
        installTee(System.out, true);
        installTee(System.err, false);
    }

    private static void installTee(PrintStream original, boolean isOut) {
        PrintStream tee = new PrintStream(original, true, StandardCharsets.UTF_8) {
            @Override public void write(byte[] b, int off, int len) {
                super.write(b, off, len);
                String line = new String(b, off, len, StandardCharsets.UTF_8);
                consoleBuffer.addLast(line);
                // Keep at most ~5 000 lines (≈ a few MB) to avoid OOM
                while (consoleBuffer.size() > 5_000) consoleBuffer.pollFirst();
            }
        };
        if (isOut) System.setOut(tee);
        else System.setErr(tee);
    }

    private static String getConsoleCapture() {
        if (consoleBuffer.isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        for (String line : consoleBuffer) sb.append(line);
        return sb.toString();
    }

    public static void uploadLog(String context) {
        if (!LunarArcConfig.isBlockMedicEnabled()) return;

        // Rate limit: never upload more often than MIN_UPLOAD_INTERVAL_MS.
        if (System.currentTimeMillis() - lastUploadTime < MIN_UPLOAD_INTERVAL_MS) return;

        // Skip if the log file hasn't changed since the last upload.
        Path logPath = findLogPath();
        if (logPath != null) {
            try {
                long modified = Files.getLastModifiedTime(logPath).toMillis();
                if (modified <= lastUploadedModified) return;
            } catch (Exception ignored) {}
        }

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

                // Record that this version of the log was uploaded.
                lastUploadTime = System.currentTimeMillis();
                if (logPath != null) {
                    try {
                        lastUploadedModified = Files.getLastModifiedTime(logPath).toMillis();
                    } catch (Exception ignored) {}
                }
            } catch (Exception e) {
                LOGGER.debug("[BlockMedic] Log upload failed: {}", e.getMessage());
            }
        });
    }

    /**
     * Uploads the latest log and returns the BlockMedic URL (for command feedback).
     * Bypasses the deduplication check — always uploads. Blocks the calling thread.
     */
    public static String uploadLogNow(String context) {
        if (!LunarArcConfig.isBlockMedicEnabled()) return null;
        try {
            String content = readLatestLog();
            if (content == null || content.isEmpty()) return null;

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
            if (!url.isEmpty()) {
                LOGGER.info("[BlockMedic] Log uploaded ({} errors detected). View at: {}",
                        errorCount.isEmpty() ? "?" : errorCount, url);
            }

            // Update the dedup and rate-limit markers.
            lastUploadTime = System.currentTimeMillis();
            Path logPath = findLogPath();
            if (logPath != null) {
                try { lastUploadedModified = Files.getLastModifiedTime(logPath).toMillis(); }
                catch (Exception ignored) {}
            }

            return url.isEmpty() ? null : url;
        } catch (Exception e) {
            LOGGER.debug("[BlockMedic] Manual upload failed: {}", e.getMessage());
            return null;
        }
    }

    static Path findLogPath() {
        // Try relative paths first, then absolute paths anchored to user.dir
        String userDir = System.getProperty("user.dir", ".");
        String[] candidates = {
            "logs/latest.log",
            "latest.log",
            userDir + "/logs/latest.log",
            userDir + "/latest.log",
            // NeoForge sometimes runs from a sub-directory
            "../logs/latest.log",
        };
        for (String candidate : candidates) {
            Path p = Paths.get(candidate);
            if (Files.exists(p)) return p;
        }
        LOGGER.debug("[BlockMedic] No log file found (user.dir={}, tried {} paths)", userDir, candidates.length);
        return null;
    }

    private static String readLatestLog() {
        Path p = findLogPath();
        if (p == null) {
            // No log file yet — fall back to captured console output
            return getConsoleCapture();
        }
        try {
            long size = Files.size(p);
            if (size > MAX_LOG_BYTES) {
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
