package io.ampznetwork.lunararc.common.telemetry;

import io.ampznetwork.lunararc.common.config.LunarArcConfig;
import io.ampznetwork.lunararc.common.server.LunarArcVersionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

public class BlockMedicReporter {

    private static final Logger LOGGER = LoggerFactory.getLogger("LunarArc-BlockMedic");
    private static final String ENDPOINT = "https://blockmedic.ampznetwork.com/api/reports";

    public static void reportCrash(String context, Throwable t) {
        if (!LunarArcConfig.isBlockMedicEnabled()) return;
        Thread.ofVirtual().name("lunararc-blockmedic").start(() -> {
            try {
                String stackTrace = buildStackTrace(t);
                String payload = buildJson(context, t.getMessage(), stackTrace);
                post(payload);
            } catch (Exception ignored) {}
        });
    }

    private static void post(String json) throws Exception {
        HttpURLConnection conn = (HttpURLConnection)
                URI.create(ENDPOINT).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("User-Agent", "LunarArc/" + LunarArcVersionInfo.projectVersion());
        conn.setDoOutput(true);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        try (OutputStream out = conn.getOutputStream()) {
            out.write(json.getBytes(StandardCharsets.UTF_8));
        }
        conn.getResponseCode(); // consume
        conn.disconnect();
    }

    private static String buildJson(String context, String message, String stackTrace) {
        return "{"
                + "\"project\":\"LunarArc\","
                + "\"version\":" + jsonStr(LunarArcVersionInfo.projectVersion()) + ","
                + "\"minecraft\":" + jsonStr(LunarArcVersionInfo.minecraftVersion()) + ","
                + "\"context\":" + jsonStr(context) + ","
                + "\"message\":" + jsonStr(message) + ","
                + "\"stackTrace\":" + jsonStr(stackTrace) + ","
                + "\"timestamp\":" + jsonStr(Instant.now().toString())
                + "}";
    }

    private static String buildStackTrace(Throwable t) {
        if (t == null) return "";
        StringBuilder sb = new StringBuilder();
        sb.append(t);
        for (StackTraceElement e : t.getStackTrace()) sb.append("\n\tat ").append(e);
        Throwable cause = t.getCause();
        if (cause != null) {
            sb.append("\nCaused by: ").append(cause);
            for (StackTraceElement e : cause.getStackTrace()) sb.append("\n\tat ").append(e);
        }
        return sb.toString();
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
