package io.ampznetwork.lunararc.launcher;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.ampznetwork.lunararc.i18n.TranslationManager;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public final class UpdateChecker {
    private static final String REPO = "AMPZNetwork/LunarArc";
    private static final String API_URL = "https://api.github.com/repos/" + REPO + "/releases";
    private static final String NO_UPDATE_MESSAGE = "No new updates available, You're up to date";
    private static final int CONNECT_TIMEOUT_MILLIS = 3000;
    private static final int READ_TIMEOUT_MILLIS = 3000;

    private UpdateChecker() {
    }

    public static String LATEST_VERSION = null;
    public static String UPDATE_URL = null;

    public static void check(String currentVersion, String buildName) {
        Path configPath = Paths.get("lunararc.conf");
        Properties props = new Properties();
        boolean enableUpdates = true;

        try {
            if (Files.exists(configPath)) {
                try (java.io.InputStream in = Files.newInputStream(configPath)) {
                    props.load(in);
                    if (!props.containsKey("enable_updates")) {
                        props.setProperty("enable_updates", "true");
                        try (java.io.OutputStream out = Files.newOutputStream(configPath)) {
                            props.store(out, "LunarArc Server Configuration");
                        }
                    }
                    enableUpdates = Boolean.parseBoolean(props.getProperty("enable_updates", "true"));
                }
            } else {
                props.setProperty("enable_updates", "true");
                try (java.io.OutputStream out = Files.newOutputStream(configPath)) {
                    props.store(out, "LunarArc Server Configuration");
                }
            }
        } catch (Exception ignored) {
        }

        if (!enableUpdates) {
            return;
        }

        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) URI.create(API_URL).toURL().openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
            connection.setReadTimeout(READ_TIMEOUT_MILLIS);
            connection.setRequestProperty("User-Agent", "LunarArc-Launcher");
            connection.setRequestProperty("Accept", "application/vnd.github+json");

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return;
            }

            JsonArray releases;
            try (InputStream input = connection.getInputStream();
                    InputStreamReader reader = new InputStreamReader(input, java.nio.charset.StandardCharsets.UTF_8)) {
                releases = JsonParser.parseReader(reader).getAsJsonArray();
            }

            boolean foundMatch = false;
            for (JsonElement element : releases) {
                JsonObject release = element.getAsJsonObject();
                if (release.has("draft") && release.get("draft").getAsBoolean()) {
                    continue;
                }

                String tagName = stringValue(release, "tag_name");
                String name = stringValue(release, "name");
                String targetCommitish = stringValue(release, "target_commitish");
                String htmlUrl = stringValue(release, "html_url");
                if (tagName.isBlank() || htmlUrl.isBlank()) {
                    continue;
                }

                String normalizedBuildName = normalize(buildName);
                String normalizedTagName = normalize(tagName);
                String normalizedReleaseName = normalize(name);
                String normalizedTarget = normalize(targetCommitish);

                boolean isMatch = normalizedBuildName.isBlank()
                        || normalizedTagName.contains(normalizedBuildName)
                        || normalizedReleaseName.contains(normalizedBuildName)
                        || normalizedTarget.contains(normalizedBuildName);

                if (!isMatch) {
                    continue;
                }

                foundMatch = true;
                if (!sameVersion(currentVersion, tagName, name, buildName)) {
                    LATEST_VERSION = tagName;
                    UPDATE_URL = htmlUrl;
                    System.out.println(TranslationManager.get("update.available", buildName, tagName, currentVersion));
                    System.out.println(TranslationManager.get("update.download", htmlUrl));
                    saveUpdateInfo(currentVersion, tagName, htmlUrl);
                } else {
                    LATEST_VERSION = null;
                    UPDATE_URL = null;
                    System.out.println(NO_UPDATE_MESSAGE);
                }
                break;
            }

            if (!foundMatch) {
                System.out.println(NO_UPDATE_MESSAGE);
            }
        } catch (Exception ignored) {

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String stringValue(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? "" : value.getAsString();
    }

    private static boolean sameVersion(String currentVersion, String tagName, String releaseName, String buildName) {
        String current = normalize(currentVersion);
        if (current.isBlank()) {
            return false;
        }

        String tag = stripBuildName(normalize(tagName), buildName);
        String name = stripBuildName(normalize(releaseName), buildName);
        return current.equals(tag) || current.equals(name) || tag.endsWith(current) || name.endsWith(current);
    }

    private static String stripBuildName(String value, String buildName) {
        String build = normalize(buildName);
        return build.isBlank() ? value : value.replace(build, "");
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static void saveUpdateInfo(String current, String latest, String url) {
        try {
            Path configPath = Paths.get("lunararc.conf");
            Properties props = new Properties();

            if (Files.exists(configPath)) {
                try (java.io.InputStream in = Files.newInputStream(configPath)) {
                    props.load(in);
                }
            }

            props.setProperty("update.current", current);
            props.setProperty("update.latest", latest);
            props.setProperty("update.url", url);

            try (java.io.OutputStream out = Files.newOutputStream(configPath)) {
                props.store(out, "LunarArc Server Configuration");
            }
        } catch (Exception ignored) {
        }
    }
}
