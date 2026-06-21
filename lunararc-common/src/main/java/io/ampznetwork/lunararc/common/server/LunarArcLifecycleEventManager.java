package io.ampznetwork.lunararc.common.server;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.handler.LifecycleEventHandler;
import net.minecraft.commands.CommandSourceStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory for the lifecycle event manager used in the hybrid environment.
 *
 * Uses a Proxy rather than directly implementing LifecycleEventManager so that we
 * never import LifecycleEventType, which does not exist as a top-level class in
 * Paper 1.21.1-R0.1-SNAPSHOT build 133.
 *
 * COMMANDS handlers are fired immediately using the server's brigadier dispatcher.
 * All other lifecycle event types are silently accepted (no-op).
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class LunarArcLifecycleEventManager {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger("LunarArc");

    // Resolve LifecycleEvents.COMMANDS at class-load time via reflection so we never
    // import LifecycleEventType (absent in build 133) or LifecycleEvents (uncertain).
    private static final Object COMMANDS_EVENT_TYPE = resolveCommandsEventType();

    private static Object resolveCommandsEventType() {
        for (String cls : List.of(
                "io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents",
                "io.papermc.paper.plugin.lifecycle.event.LifecycleEvents")) {
            try {
                return Class.forName(cls, true,
                        LunarArcLifecycleEventManager.class.getClassLoader())
                        .getField("COMMANDS").get(null);
            } catch (Throwable ignored) {}
        }
        return null;
    }

    public static LifecycleEventManager<Plugin> create() {
        return (LifecycleEventManager<Plugin>) java.lang.reflect.Proxy.newProxyInstance(
                LifecycleEventManager.class.getClassLoader(),
                new Class<?>[]{LifecycleEventManager.class},
                (proxy, method, args) -> {
                    if ("registerEventHandler".equals(method.getName()) && args != null) {
                        Object eventType = args[0];
                        if (COMMANDS_EVENT_TYPE != null && eventType == COMMANDS_EVENT_TYPE
                                && args.length >= 2 && args[1] instanceof LifecycleEventHandler handler) {
                            fireCommandsHandler(handler);
                        }
                        // args.length == 1: LifecycleEventHandlerConfiguration form — handled below
                        if (args.length == 1 && COMMANDS_EVENT_TYPE != null) {
                            tryFireFromConfig(args[0]);
                        }
                    }
                    // Guard primitive return types to prevent NullPointerException on unboxing
                    Class<?> ret = method.getReturnType();
                    if (ret == boolean.class) return false;
                    if (ret == int.class || ret == long.class || ret == double.class
                            || ret == float.class || ret == short.class || ret == byte.class) return 0;
                    return null;
                });
    }

    private static void tryFireFromConfig(Object config) {
        try {
            // LifecycleEventHandlerConfiguration has eventType() and handler() methods
            java.lang.reflect.Method eventTypeMethod = config.getClass().getMethod("eventType");
            Object eventType = eventTypeMethod.invoke(config);
            if (eventType == COMMANDS_EVENT_TYPE) {
                java.lang.reflect.Method handlerMethod = config.getClass().getMethod("handler");
                Object handler = handlerMethod.invoke(config);
                if (handler instanceof LifecycleEventHandler h) {
                    fireCommandsHandler(h);
                }
            }
        } catch (Exception ignored) {}
    }

    private static void fireCommandsHandler(LifecycleEventHandler handler) {
        try {
            org.bukkit.craftbukkit.v1_21_R1.CraftServer craftServer =
                    (org.bukkit.craftbukkit.v1_21_R1.CraftServer) org.bukkit.Bukkit.getServer();
            if (craftServer == null) {
                log.warn("[LunarArc] Cannot fire COMMANDS lifecycle event: server not yet initialized");
                return;
            }
            com.mojang.brigadier.CommandDispatcher<CommandSourceStack> dispatcher =
                    craftServer.getHandle().getCommands().getDispatcher();
            Commands cmds = makeCommandsProxy(dispatcher);
            io.papermc.paper.plugin.lifecycle.event.LifecycleEvent event = makeCommandsEvent(cmds);
            handler.run(event);
        } catch (Throwable e) {
            log.warn("[LunarArc] Could not fire COMMANDS lifecycle event", e);
        }
    }

    private static Commands makeCommandsProxy(
            com.mojang.brigadier.CommandDispatcher<CommandSourceStack> dispatcher) {
        return (Commands) java.lang.reflect.Proxy.newProxyInstance(
                Commands.class.getClassLoader(),
                new Class<?>[]{Commands.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getDispatcher" -> { return dispatcher; }
                        case "register" -> {
                            if (args != null && args.length >= 1) {
                                try {
                                    if (args[0] instanceof com.mojang.brigadier.builder.LiteralArgumentBuilder<?> b) {
                                        return dispatcher.register(
                                                (com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack>) b);
                                    } else if (args[0] instanceof com.mojang.brigadier.tree.LiteralCommandNode<?> n) {
                                        var node = (com.mojang.brigadier.tree.LiteralCommandNode<CommandSourceStack>) n;
                                        dispatcher.getRoot().addChild(node);
                                        return node;
                                    }
                                } catch (Exception ignored) {}
                            }
                            return null;
                        }
                        default -> {
                            Class<?> r = method.getReturnType();
                            if (r == boolean.class) return false;
                            if (r == int.class) return 0;
                            if (r == java.util.Collection.class || r == java.util.List.class)
                                return java.util.Collections.emptyList();
                            return null;
                        }
                    }
                });
    }

    private static io.papermc.paper.plugin.lifecycle.event.LifecycleEvent makeCommandsEvent(Commands cmds) {
        List<Class<?>> ifaces = new ArrayList<>();
        ifaces.add(io.papermc.paper.plugin.lifecycle.event.LifecycleEvent.class);
        for (String name : List.of(
                "io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent",
                "io.papermc.paper.plugin.lifecycle.event.registrar.RegistrarEvent")) {
            try {
                ifaces.add(Class.forName(name, true, Commands.class.getClassLoader()));
            } catch (ClassNotFoundException ignored) {}
        }
        return (io.papermc.paper.plugin.lifecycle.event.LifecycleEvent) java.lang.reflect.Proxy.newProxyInstance(
                Commands.class.getClassLoader(),
                ifaces.toArray(new Class<?>[0]),
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "registrar" -> { return cmds; }
                        case "cause" -> { return getCauseInitial(); }
                        case "hashCode" -> { return System.identityHashCode(proxy); }
                        case "equals" -> { return proxy == args[0]; }
                        case "toString" -> { return "LunarArcCommandsEvent"; }
                        default -> { return null; }
                    }
                });
    }

    private static Object getCauseInitial() {
        try {
            Class<?> causeClass = Class.forName(
                    "io.papermc.paper.plugin.lifecycle.event.registrar.ReloadableRegistrarEvent$Cause",
                    true, Commands.class.getClassLoader());
            return causeClass.getField("INITIAL").get(null);
        } catch (Exception ignored) {
            return null;
        }
    }
}
