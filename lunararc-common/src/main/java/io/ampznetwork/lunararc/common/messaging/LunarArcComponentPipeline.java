package io.ampznetwork.lunararc.common.messaging;

import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.chat.BaseComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.craftbukkit.v1_21_R1.util.CraftChatMessage;

/**
 * The single authoritative outgoing in-game component pipeline for LunarArc.
 *
 * <p>Keep this class deliberately free of eager serializer initialization.
 * Hybrid servers can load plugin-facing Adventure/Bungee classes through a
 * different class-loader path during bootstrap; eagerly resolving one optional
 * serializer used to poison this class with ExceptionInInitializerError and all
 * later calls then failed as NoClassDefFoundError. Every conversion therefore
 * resolves its adapter at call time and has a safe native fallback.</p>
 */
public final class LunarArcComponentPipeline {
    private LunarArcComponentPipeline() {}

    public static net.minecraft.network.chat.Component fromLegacy(String message) {
        if (message == null || message.isEmpty()) {
            return net.minecraft.network.chat.Component.empty();
        }

        // Preferred path preserves legacy colours/styles including §x RGB.
        try {
            net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer serializer =
                    net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.builder()
                            .character('§')
                            .hexColors()
                            .useUnusualXRepeatedCharacterHexFormat()
                            .build();
            return fromAdventure(serializer.deserialize(message));
        } catch (Throwable ignored) {
            // CraftChatMessage has its own guarded legacy conversion and native
            // literal fallback, so the messaging class can never become poisoned.
            try {
                net.minecraft.network.chat.Component converted = CraftChatMessage.fromStringOrNull(message);
                if (converted != null) return converted;
            } catch (Throwable ignoredAgain) {
            }
            return net.minecraft.network.chat.Component.literal(message);
        }
    }

    public static net.minecraft.network.chat.Component fromAdventure(Component component) {
        if (component == null) {
            return net.minecraft.network.chat.Component.empty();
        }

        // Paper's native Adventure bridge is the best conversion when present.
        try {
            Class<?> bridge = Class.forName("io.papermc.paper.adventure.PaperAdventure", false,
                    LunarArcComponentPipeline.class.getClassLoader());
            java.lang.reflect.Method method = bridge.getMethod("asVanilla", Component.class);
            Object converted = method.invoke(null, component);
            if (converted instanceof net.minecraft.network.chat.Component nms) return nms;
        } catch (Throwable ignored) {
        }

        // Gson remains the portable shared-runtime conversion path.
        try {
            String json = net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson().serialize(component);
            net.minecraft.network.chat.MutableComponent nms = CraftChatMessage.fromJSON(json);
            if (nms != null) return nms;
        } catch (Throwable ignored) {
        }

        // Last-resort plain conversion must itself be guarded: the plain serializer
        // may live in the same optional Adventure serializer module on some setups.
        try {
            String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(component);
            return net.minecraft.network.chat.Component.literal(plain);
        } catch (Throwable ignored) {
            return net.minecraft.network.chat.Component.literal(String.valueOf(component));
        }
    }

    public static net.minecraft.network.chat.Component fromBungee(BaseComponent... components) {
        if (components == null || components.length == 0) {
            return net.minecraft.network.chat.Component.empty();
        }
        try {
            String json = net.md_5.bungee.chat.ComponentSerializer.toString(components);
            net.minecraft.network.chat.MutableComponent nms = CraftChatMessage.fromJSON(json);
            if (nms != null) return nms;
        } catch (Throwable ignored) {
        }
        try {
            return fromLegacy(BaseComponent.toLegacyText(components));
        } catch (Throwable ignored) {
            return net.minecraft.network.chat.Component.literal("");
        }
    }

    public static void sendSystem(ServerPlayer player, String legacy) {
        sendSystem(player, fromLegacy(legacy));
    }

    public static void sendSystem(ServerPlayer player, Component component) {
        sendSystem(player, fromAdventure(component));
    }

    public static void sendSystem(ServerPlayer player, BaseComponent... components) {
        sendSystem(player, fromBungee(components));
    }

    public static void sendSystem(ServerPlayer player, net.minecraft.network.chat.Component component) {
        if (player == null || player.connection == null || component == null) return;
        player.connection.send(new ClientboundSystemChatPacket(component, false));
    }

    public static void sendActionBar(ServerPlayer player, String legacy) {
        sendActionBar(player, fromLegacy(legacy));
    }

    public static void sendActionBar(ServerPlayer player, Component component) {
        sendActionBar(player, fromAdventure(component));
    }

    public static void sendActionBar(ServerPlayer player, BaseComponent... components) {
        sendActionBar(player, fromBungee(components));
    }

    public static void sendActionBar(ServerPlayer player, net.minecraft.network.chat.Component component) {
        if (player == null || player.connection == null || component == null) return;
        player.connection.send(new ClientboundSetActionBarTextPacket(component));
    }
}
