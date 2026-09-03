package io.ampznetwork.lunararc.common.bridge;

/**
 * Exposes the two pieces of explosion state CraftBukkit adds to {@code net.minecraft.world.level.
 * Explosion} as extra fields and that LunarArc cannot add by patching, since the Minecraft runtime
 * belongs to the loader.
 *
 * <p>{@code yield} is the fraction of destroyed blocks that drop as items. Vanilla has no such
 * field - it decides drops from the explosion's {@code BlockInteraction} and radius - but the
 * Bukkit API lets a plugin read and rewrite it on the explosion event, so it has to live
 * somewhere.</p>
 */
public interface ExplosionBridge {

    float lunararc$getYield();

    void lunararc$setYield(float yield);
}
