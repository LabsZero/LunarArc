package io.ampznetwork.lunararc.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Opt-in tracing for the plugin compatibility layer, for use while LunarArc is being built.
 *
 * <p>Every plugin failure this project has had to diagnose so far arrived as a message from the
 * <em>plugin</em>, describing a symptom, with nothing said about what LunarArc did on its way
 * there. "Could not find field 'e' in class ServerCommonPacketListenerImpl" does not distinguish
 * between a missing mapping, a mapping that was found and wrong, and a lookup that never reached
 * the remapper at all - the last of which is what it actually was. That took a source read of
 * another project to establish. These channels answer it directly.</p>
 *
 * <p>Off unless asked for:</p>
 *
 * <pre>
 *   -Dlunararc.debug=reflect            one channel
 *   -Dlunararc.debug=reflect,classload  several
 *   -Dlunararc.debug=all                everything
 * </pre>
 *
 * <p>Each channel is a {@code static final boolean} resolved once at class initialization, so a
 * disabled channel is a constant {@code false} the JIT folds away along with the call behind it.
 * That is why every call site is written as {@code if (LunarArcDebug.REFLECT) ...} - the guard is
 * what makes this free to leave in place, and an unguarded call would still build its arguments.</p>
 *
 * <p>Logging goes to {@code LunarArc/Debug} at INFO. Not DEBUG: the loaders ship their own log4j
 * configuration and the level is theirs to filter, so a channel someone deliberately turned on
 * would silently produce nothing on a stock server. Turning it on is the filter.</p>
 *
 * <p>This is a development aid. It is safe to ship - it does nothing unless the property is set -
 * but it is not an API, and the channels are expected to come and go with whatever is being worked
 * on.</p>
 */
public final class LunarArcDebug {

    private static final Logger LOGGER = LoggerFactory.getLogger("LunarArc/Debug");

    /** Reflective member lookups routed through LunarArcReflectionBridge: what a plugin asked for, what it was mapped to, and whether it resolved. */
    public static final boolean REFLECT;

    /** Bytecode transformation of plugin classes: which remapping each class was given, and why. */
    public static final boolean REMAP;

    /** Plugin class loading: which loader answered a name, and under which of the requested or mapped spellings. */
    public static final boolean CLASSLOAD;

    static {
        String raw = System.getProperty("lunararc.debug", "");
        java.util.Set<String> channels = new java.util.HashSet<>();
        for (String part : raw.split(",")) {
            String trimmed = part.trim().toLowerCase(java.util.Locale.ROOT);
            if (!trimmed.isEmpty()) channels.add(trimmed);
        }
        boolean all = channels.contains("all");
        REFLECT = all || channels.contains("reflect");
        REMAP = all || channels.contains("remap");
        CLASSLOAD = all || channels.contains("classload");

        if (REFLECT || REMAP || CLASSLOAD) {
            StringBuilder enabled = new StringBuilder();
            if (REFLECT) enabled.append(" reflect");
            if (REMAP) enabled.append(" remap");
            if (CLASSLOAD) enabled.append(" classload");
            LOGGER.info("LunarArc debug channels enabled:{}. This is verbose by design and is not "
                    + "meant to be left on for a running server.", enabled);
        }
    }

    private LunarArcDebug() {
    }

    /** Log on the reflect channel. Call behind {@code if (LunarArcDebug.REFLECT)}. */
    public static void reflect(String format, Object... args) {
        LOGGER.info("[reflect] " + format, args);
    }

    /** Log on the remap channel. Call behind {@code if (LunarArcDebug.REMAP)}. */
    public static void remap(String format, Object... args) {
        LOGGER.info("[remap] " + format, args);
    }

    /** Log on the classload channel. Call behind {@code if (LunarArcDebug.CLASSLOAD)}. */
    public static void classload(String format, Object... args) {
        LOGGER.info("[classload] " + format, args);
    }

    /**
     * The class that called into the bridge, skipping LunarArc's own frames.
     *
     * <p>This is the field that mattered and was missing: a reflective failure names the member
     * but not who looked it up, and "which plugin class is doing this" is what decides whether the
     * lookup is even reaching the remapper. DecentHolograms routes every one of its lookups
     * through a single {@code ReflectField} helper, which is both why its class carried no
     * net.minecraft reference for the old constant-pool test to find and, once named here, the
     * fastest way to see that.</p>
     *
     * <p>Walks the stack, so only ever call it behind an enabled channel.</p>
     */
    public static String caller() {
        return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                .walk(frames -> frames
                        .map(StackWalker.StackFrame::getDeclaringClass)
                        .filter(type -> !type.getName().startsWith("io.ampznetwork.lunararc."))
                        .filter(type -> !type.getName().startsWith("java."))
                        .map(Class::getName)
                        .findFirst()
                        .orElse("unknown"));
    }
}
