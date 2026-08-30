package io.papermc.paper.configuration;

import io.papermc.paper.configuration.type.DurationOrDisabled;

/**
 * Real Paper's {@code WorldConfiguration} is its entire per-world configuration system —
 * hundreds of fields, loaded per-world from paper-world.yml via Configurate. That's a much
 * larger undertaking than anything ported here (same scope decision as
 * {@link PaperConfigurations} elsewhere in this package).
 * <p>
 * This class intentionally contains only the real {@code lootables} section
 * {@code PaperLootableInventoryData} actually reads, with every field at its exact real Paper
 * default (verified against patches/server/0005-Paper-config-files.patch). Every
 * {@link net.minecraft.world.level.Level} shares one static instance (see the Level.paperConfig()
 * mixin) rather than per-world customized config, since real per-world config-file loading isn't
 * wired up. Nothing else from Paper's real config system should be assumed to exist here.
 */
public final class WorldConfiguration {

    /**
     * Real Paper reaches this per-Level via Level.paperConfig(). Since every Level shares this
     * exact same instance here anyway (see the class javadoc), exposing it as a plain static
     * field lets any file reference it directly, avoiding the fact that a Mixin-added
     * Level.paperConfig() method wouldn't be visible to separately-compiled callers at
     * compile time (Mixin only weaves it into bytecode, not into the source-level type other
     * files see).
     */
    public static final WorldConfiguration CURRENT = new WorldConfiguration();

    public final Lootables lootables = new Lootables();

    public static final class Lootables {
        public DurationOrDisabled restrictPlayerRelootTime = DurationOrDisabled.USE_DISABLED;
        public boolean restrictPlayerReloot = true;
        public boolean autoReplenish = false;
        public int maxRefills = -1;
        public io.papermc.paper.configuration.type.Duration refreshMin = io.papermc.paper.configuration.type.Duration.of("12h");
        public io.papermc.paper.configuration.type.Duration refreshMax = io.papermc.paper.configuration.type.Duration.of("2d");
        public boolean resetSeedOnFill = true;
    }
}
