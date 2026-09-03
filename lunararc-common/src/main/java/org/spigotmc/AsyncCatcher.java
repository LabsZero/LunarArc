package org.spigotmc;

/**
 * Spigot/Paper compatibility entry point for primary-thread enforcement.
 *
 * <p>LunarArc keeps the actual policy in common code so the behaviour is
 * identical on Forge, NeoForge, Fabric and Quilt.</p>
 */
public final class AsyncCatcher {
    /**
     * Kept for binary/source compatibility with plugins and server integrations
     * that temporarily disable the Spigot async catcher.
     */
    public static boolean enabled = true;

    private AsyncCatcher() {
    }

    public static void catchOp(String reason) {
        io.ampznetwork.lunararc.common.util.AsyncCatcher.enabled = enabled;
        io.ampznetwork.lunararc.common.util.AsyncCatcher.catchOp(reason);
    }
}
