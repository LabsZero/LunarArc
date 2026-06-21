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
                doUpload(content, context, logPath);
            } catch (Exception e) {
                LOGGER.warn("[BlockMedic] Auto-upload failed: {} — {}", e.getClass().getSimpleName(), e.getMessage());
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
            Path logPath = findLogPath();
            if (logPath == null) {
                LOGGER.warn("[BlockMedic] No log file found. Tried logs/latest.log relative to user.dir={}. "
                        + "Falling back to console capture.", System.getProperty("user.dir", "?"));
            }
            String content = logPath != null ? readFile(logPath) : getConsoleCapture();
            if (content == null || content.isEmpty()) {
                LOGGER.warn("[BlockMedic] Nothing to upload — log file is empty and console capture is empty.");
                return null;
            }
            return doUpload(content, context, logPath);
        } catch (Exception e) {
            LOGGER.warn("[BlockMedic] Upload failed: {} — {}", e.getClass().getSimpleName(), e.getMessage());
            return null;
        }
    }

    /**
     * Finds the most recent crash report in the crash-reports/ directory.
     * Returns null if none exist.
     */
    public static Path findCrashReport() {
        String userDir = System.getProperty("user.dir", ".");
        String[] dirs = {"crash-reports", userDir + "/crash-reports", "../crash-reports"};
        Path latest = null;
        long latestTime = 0;
        for (String dir : dirs) {
            Path d = Paths.get(dir);
            if (!Files.isDirectory(d)) continue;
            try (var stream = Files.list(d)) {
                for (Path p : stream.toList()) {
                    String name = p.getFileName().toString().toLowerCase();
                    if (!name.endsWith(".txt") && !name.endsWith(".log")) continue;
                    long mod = Files.getLastModifiedTime(p).toMillis();
                    if (mod > latestTime) { latestTime = mod; latest = p; }
                }
            } catch (Exception ignored) {}
        }
        return latest;
    }

    /**
     * Uploads a specific file path to BlockMedic synchronously.
     * Returns the view URL or null on failure.
     */
    public static String uploadFileNow(Path filePath, String context) {
        if (!LunarArcConfig.isBlockMedicEnabled()) return null;
        try {
            String content = readFile(filePath);
            if (content == null || content.isEmpty()) return null;
            return doUpload(content, context, filePath);
        } catch (Exception e) {
            LOGGER.debug("[BlockMedic] Upload failed: {}", e.getMessage());
            return null;
        }
    }

    static Path findLogPath() {
        String userDir = System.getProperty("user.dir", ".");
        String[] candidates = {
            "logs/latest.log",
            "latest.log",
            userDir + "/logs/latest.log",
            userDir + "/latest.log",
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
        if (p == null) return getConsoleCapture();
        return readFile(p);
    }

    private static String readFile(Path p) {
        if (p == null || !Files.exists(p)) return null;
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

    private static String doUpload(String content, String context, Path sourcePath) throws Exception {
        String metadata = "["
                + metaEntry("version", LunarArcVersionInfo.projectVersion(), "LunarArc Version", true) + ","
                + metaEntry("minecraft", LunarArcVersionInfo.minecraftVersion(), "Minecraft Version", true) + ","
                + metaEntry("context", context, "Context", true)
                + "]";
        String payload = "{\"content\":" + jsonStr(content) + ",\"source\":\"LunarArc\",\"metadata\":" + metadata + "}";
        String response = post(payload);
        String url = extract(response, "url");
        String errorCount = extract(response, "errors");
        if (!url.isEmpty()) {
            LOGGER.info("[BlockMedic] Uploaded {} ({} errors). View at: {}",
                    sourcePath != null ? sourcePath.getFileName() : "console", errorCount.isEmpty() ? "?" : errorCount, url);
        }
        lastUploadTime = System.currentTimeMillis();
        if (sourcePath != null) {
            try { lastUploadedModified = Files.getLastModifiedTime(sourcePath).toMillis(); } catch (Exception ignored) {}
        }
        return url.isEmpty() ? null : url;
    }

    private static String post(String json) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) URI.create(ENDPOINT).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("User-Agent", "LunarArc/" + LunarArcVersionInfo.projectVersion());
        conn.setRequestProperty("Origin", "https://ampznetwork.com");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(10_000);
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        conn.setRequestProperty("Content-Length", String.valueOf(body.length));
        try (OutputStream out = conn.getOutputStream()) {
            out.write(body);
        }
        int status = conn.getResponseCode();
        if (status >= 200 && status < 300) {
            try (InputStream in = conn.getInputStream()) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        // Read error body so callers can log it
        String errorBody = "";
        try (InputStream err = conn.getErrorStream()) {
            if (err != null) errorBody = new String(err.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception ignored) {}
        throw new Exception("HTTP " + status + ": " + errorBody);
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
        StringBuilder sb = new StringBuilder(s.length() + 16);
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"'  -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) {
                        // Escape other control characters (null bytes, etc.)
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
        return sb.toString();
    }
}
