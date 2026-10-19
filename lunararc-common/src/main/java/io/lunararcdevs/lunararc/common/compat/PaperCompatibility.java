package io.lunararcdevs.lunararc.common.compat;

/**
 * Shared Paper compatibility policy: legacy 1.x plugins plus the 26.x year-based versions up to the runtime.
 */
public final class PaperCompatibility {
    public static final String RUNTIME_MINECRAFT = "26.3";
    public static final String RUNTIME_API = "26.3";

    private PaperCompatibility() {
    }

    public static boolean isSupportedApiVersion(String apiVersion) {
        if (apiVersion == null || apiVersion.isBlank()) {
            return true; // legacy plugin.yml behaviour
        }
        String normalized = apiVersion.trim();
        if (normalized.startsWith("1.")) return true; // pre-26 plugins are handled by the remapper
        String[] parts = normalized.split("[.]");
        String[] runtime = RUNTIME_API.split("[.]");
        try {
            int major = Integer.parseInt(parts[0]);
            int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            int runtimeMajor = Integer.parseInt(runtime[0]);
            int runtimeMinor = Integer.parseInt(runtime[1]);
            return major < runtimeMajor || (major == runtimeMajor && minor <= runtimeMinor);
        } catch (NumberFormatException exception) {
            return false;
        }
    }
}
