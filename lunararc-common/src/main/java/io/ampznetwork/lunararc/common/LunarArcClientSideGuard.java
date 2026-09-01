package io.ampznetwork.lunararc.common;

/**
 * Refuses to start when LunarArc has been installed into a client.
 *
 * <p>LunarArc is a dedicated-server platform: it implements the Bukkit/Spigot/Paper plugin server
 * on top of a mod loader. Nothing in it has client-side behaviour. Dropped into a client modpack it
 * cannot do anything useful, and the way it fails without this guard is unhelpful - on Fabric the
 * mod is declared server-only and is quietly skipped, leaving no trace of why nothing happened,
 * while on the others it starts wiring up a plugin server that has no dedicated server to attach
 * to and fails later somewhere unrelated.</p>
 *
 * <p>Each loader module decides for itself whether it is running on a client, because only the
 * loader can answer that, and hands the answer here so every platform reports the same thing in
 * the same words.</p>
 */
public final class LunarArcClientSideGuard {

    private LunarArcClientSideGuard() {
    }

    /**
     * Thrown when LunarArc is loaded on a client. Kept as its own type so the message is
     * recognisable in a crash report or a mod-loading error screen rather than reading like an
     * internal fault.
     */
    public static final class ClientSideNotSupportedException extends IllegalStateException {
        private static final long serialVersionUID = 1L;

        ClientSideNotSupportedException(String message) {
            super(message);
        }
    }

    /**
     * Stops startup if {@code clientSide} is true.
     *
     * @param platform    the loader's own name, used in the message so the reader can tell which
     *                    of the four installs they are looking at
     * @param clientSide  whether the loader reports this process as a client
     */
    public static void requireDedicatedServer(String platform, boolean clientSide) {
        if (!clientSide) return;
        throw new ClientSideNotSupportedException(message(platform));
    }

    /** The wording shown to whoever installed it, kept in one place for all four loaders. */
    public static String message(String platform) {
        return """

                LunarArc is a dedicated-server platform, not a client mod.

                This build runs a Bukkit/Spigot/Paper plugin server on top of %s, and only works \
                when a dedicated server starts it. It has no client-side behaviour at all, so it \
                cannot run in a client instance or a singleplayer world.

                To fix this: remove LunarArc from this instance's mods folder.

                To use LunarArc: install it into a dedicated server instead. Players join that \
                server with an ordinary unmodified client - they do not need this mod, or any \
                other, to connect.
                """.formatted(platform);
    }
}
