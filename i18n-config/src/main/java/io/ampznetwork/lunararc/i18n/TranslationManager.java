package io.ampznetwork.lunararc.i18n;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

public class TranslationManager {
    private static final String DEFAULT_LOCALE = "en_gb";
    private static final Map<String, JsonObject> cache = new java.util.concurrent.ConcurrentHashMap<>();
    private static volatile String currentLocale = DEFAULT_LOCALE;

    static {

        String lang = Locale.getDefault().getLanguage().toLowerCase();
        String country = Locale.getDefault().getCountry().toLowerCase();
        currentLocale = lang + "_" + country;

        loadLocale(DEFAULT_LOCALE);
    }

    public static void setLocale(String locale) {
        currentLocale = locale.toLowerCase();
        loadLocale(currentLocale);
    }

    private static void loadLocale(String locale) {
        if (cache.containsKey(locale))
            return;

        try (InputStream in = openLocale(locale)) {
            if (in != null) {
                JsonObject json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8))
                        .getAsJsonObject();
                cache.put(locale, json);
            } else if (!locale.equals(DEFAULT_LOCALE)) {
                loadLocale(DEFAULT_LOCALE);
            }
        } catch (Exception e) {
            if (!locale.equals(DEFAULT_LOCALE)) {
                loadLocale(DEFAULT_LOCALE);
            }
        }
    }

    private static InputStream openLocale(String locale) {
        String resource = "locale/" + locale + ".json";
        ClassLoader own = TranslationManager.class.getClassLoader();
        if (own != null) {
            InputStream in = own.getResourceAsStream(resource);
            if (in != null) return in;
        }

        ClassLoader context = Thread.currentThread().getContextClassLoader();
        if (context != null && context != own) {
            InputStream in = context.getResourceAsStream(resource);
            if (in != null) return in;
        }

        return ClassLoader.getSystemResourceAsStream(resource);
    }

    public static String get(String key, Object... args) {
        String translation = getRaw(currentLocale, key);
        if (translation == null && !currentLocale.equals(DEFAULT_LOCALE)) {
            translation = getRaw(DEFAULT_LOCALE, key);
        }

        if (translation == null)
            return key;

        for (int i = 0; i < args.length; i++) {
            translation = translation.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return translation;
    }

    private static String getRaw(String locale, String key) {
        JsonObject json = cache.get(locale);
        if (json == null) {
            loadLocale(locale);
            json = cache.get(locale);
        }
        if (json != null && json.has(key)) {
            return json.get(key).getAsString();
        }
        return null;
    }
}
