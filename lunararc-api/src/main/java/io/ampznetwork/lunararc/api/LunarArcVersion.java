package io.ampznetwork.lunararc.api;

public final class LunarArcVersion {

    private static volatile LunarArcVersion current;

    private final String minecraftVersion;
    private final String lunarArcVersion;
    private final String paperApiVersion;
    private final int paperBuild;

    public LunarArcVersion(String minecraftVersion, String lunarArcVersion, String paperApiVersion, int paperBuild) {
        this.minecraftVersion = minecraftVersion;
        this.lunarArcVersion = lunarArcVersion;
        this.paperApiVersion = paperApiVersion;
        this.paperBuild = paperBuild;
    }

    public String minecraftVersion() {
        return minecraftVersion;
    }

    public String lunarArcVersion() {
        return lunarArcVersion;
    }

    public String paperApiVersion() {
        return paperApiVersion;
    }

    public int paperBuild() {
        return paperBuild;
    }

    @Override
    public String toString() {
        return "LunarArcVersion{" +
                "minecraft='" + minecraftVersion + '\'' +
                ", lunarArc='" + lunarArcVersion + '\'' +
                ", paperApi='" + paperApiVersion + '\'' +
                ", paperBuild=" + paperBuild +
                '}';
    }

    public static LunarArcVersion current() {
        LunarArcVersion version = current;
        if (version == null) throw new IllegalStateException("Version is not set!");
        return version;
    }

    public static void setCurrent(LunarArcVersion version) {
        if (current != null) throw new IllegalStateException("Version is already set!");
        if (version == null) throw new IllegalArgumentException("Version cannot be null!");
        current = version;
    }
}
