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
     * @param clientSide whether the loader reports this process as a client
     */
    public static void requireDedicatedServer(boolean clientSide) {
        if (!clientSide) return;
        throw new ClientSideNotSupportedException(MESSAGE);
    }

    /**
     * What the person who installed it reads. Deliberately short: it appears in a loader's error
     * popup, where the only things that help are what is wrong and what to do about it. Someone
     * who has put a server jar in a client instance does not need the platform explained.
     */
    public static final String MESSAGE = """

            LunarArc is a server-only mod and is not needed on the client.

            Remove it from this instance's mods folder to continue.
            """;
}
