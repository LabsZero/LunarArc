package io.ampznetwork.lunararc.common.server;

import com.destroystokyo.paper.util.VersionFetcher;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class LunarArcVersionFetcher implements VersionFetcher {

    private static final String API_URL = "https://api.github.com/repos/AMPZNetwork/LunarArc/releases";
    private static final int CONNECT_TIMEOUT_MILLIS = 3000;
    private static final int READ_TIMEOUT_MILLIS = 3000;

    @Override
    public long getCacheTime() {
        return TimeUnit.HOURS.toMillis(1);
    }

    @Override
    public Component getVersionMessage(String serverVersion) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) URI.create(API_URL).toURL().openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
            connection.setReadTimeout(READ_TIMEOUT_MILLIS);
            connection.setRequestProperty("User-Agent", "LunarArc-VersionCommand");
            connection.setRequestProperty("Accept", "application/vnd.github+json");

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return Component.text("Could not check for updates (GitHub returned "
                        + connection.getResponseCode() + ").", NamedTextColor.RED);
            }

            JsonArray releases;
            try (InputStream input = connection.getInputStream();
                 InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
                releases = JsonParser.parseReader(reader).getAsJsonArray();
            }

            for (JsonElement element : releases) {
                JsonObject release = element.getAsJsonObject();
                if (release.has("draft") && release.get("draft").getAsBoolean()) continue;

                String tagName = stringValue(release, "tag_name");
                String htmlUrl = stringValue(release, "html_url");
                if (tagName.isBlank() || htmlUrl.isBlank()) continue;

                if (isSameVersion(serverVersion, tagName)) {
                    return Component.text("You are running the latest version of LunarArc ("
                            + serverVersion + ").", NamedTextColor.GREEN);
                }

                return Component.text("A new version of LunarArc is available: " + tagName
                        + " (you are running " + serverVersion + "). Download: " + htmlUrl, NamedTextColor.YELLOW);
            }

            return Component.text("No LunarArc releases found to compare against.", NamedTextColor.GRAY);
        } catch (Exception e) {
            return Component.text("Could not check for updates: " + e.getMessage(), NamedTextColor.RED);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static String stringValue(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? "" : value.getAsString();
    }

    private static boolean isSameVersion(String serverVersion, String tagName) {
        String current = normalize(serverVersion);
        String tag = normalize(tagName);
        return !current.isBlank() && (current.equals(tag) || tag.endsWith(current) || current.endsWith(tag));
    }

    private static String normalize(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}