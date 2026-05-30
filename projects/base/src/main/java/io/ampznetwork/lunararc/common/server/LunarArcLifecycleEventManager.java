package io.ampznetwork.lunararc.common.server;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventType;
import io.papermc.paper.plugin.lifecycle.event.handler.LifecycleEventHandler;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.minecraft.commands.CommandSourceStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Lifecycle event manager for the hybrid environment.
 * COMMANDS handlers are fired immediately using the server's brigadier dispatcher
 * so that plugins registering commands via the Paper lifecycle API work correctly.
 * All other lifecycle event types are accepted silently (no-op).
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class LunarArcLifecycleEventManager implements LifecycleEventManager<Plugin> {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger("LunarArc");

    @Override
    public <E extends io.papermc.paper.plugin.lifecycle.event.LifecycleEvent> void registerEventHandler(
            LifecycleEventType<? super Plugin, ? extends E, ?> eventType,
            LifecycleEventHandler<? super E> eventHandler) {
        if (eventType == LifecycleEvents.COMMANDS) {
            fireCommandsHandler((LifecycleEventHandler) eventHandler);
        }
        // All other lifecycle event types are silently accepted (no-op)
    }

    private void fireCommandsHandler(LifecycleEventHandler handler) {
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
        } catch (Exception e) {
            log.warn("[LunarArc] Could not fire COMMANDS lifecycle event: {}", e.getMessage());
        }
    }

    private static Commands makeCommandsProxy(
            com.mojang.brigadier.CommandDispatcher<CommandSourceStack> dispatcher) {
        return (Commands) java.lang.reflect.Proxy.newProxyInstance(
            Commands.class.getClassLoader(),
            new Class<?>[]{ Commands.class },
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
            }
        );
    }

    private static io.papermc.paper.plugin.lifecycle.event.LifecycleEvent makeCommandsEvent(Commands cmds) {
        List<Class<?>> ifaces = new ArrayList<>();
        ifaces.add(io.papermc.paper.plugin.lifecycle.event.LifecycleEvent.class);
        // Add ReloadableRegistrarEvent / RegistrarEvent interfaces if available on this build
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
            }
        );
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
