package io.ampznetwork.lunararc.api;

public enum LunarArcPlatform {
    FORGE,
    NEOFORGE,
    FABRIC,
    QUILT,
    UNKNOWN;

    public static LunarArcPlatform fromPlatformName(String name) {
        if (name == null) return UNKNOWN;
        return switch (name.toLowerCase(java.util.Locale.ROOT)) {
            case "forge" -> FORGE;
            case "neoforge" -> NEOFORGE;
            case "fabric" -> FABRIC;
            case "quilt" -> QUILT;
            default -> UNKNOWN;
        };
    }
}
