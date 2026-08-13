package org.bukkit.craftbukkit.v1_21_R1.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class CraftChatMessage {
    private CraftChatMessage() {}

    public static Component fromStringOrNull(String message) {
        if (message == null || message.isEmpty()) return null;
        try {
            net.kyori.adventure.text.Component adventure =
                    net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                            .legacySection().deserialize(message);
            String json = net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
                    .gson().serialize(adventure);
            MutableComponent parsed = fromJSON(json);
            if (parsed != null) return parsed;
        } catch (Throwable ignored) {
        }
        return Component.literal(message);
    }

    public static Component[] fromString(String message) {
        return new Component[] { fromStringOrNull(message) };
    }

    public static String fromComponent(Component component) {
        if (component == null) return "";
        return component.getString();
    }
    
    private static net.minecraft.core.HolderLookup.Provider lookupProvider() {
        try {
            io.ampznetwork.lunararc.common.LunarArcPlatform.getServer().getServer().registryAccess();
            return io.ampznetwork.lunararc.common.LunarArcPlatform.getServer().getServer().registryAccess();
        } catch (Throwable ignored) {
            return net.minecraft.core.HolderLookup.Provider.create(java.util.stream.Stream.empty());
        }
    }

    public static MutableComponent fromJSON(String json) {
        if (json == null || json.isEmpty()) return Component.empty();
        return Component.Serializer.fromJson(json, lookupProvider());
    }
    
    public static String toJSON(Component component) {
        return Component.Serializer.toJson(component, lookupProvider());
    }
    
    public static final class ChatSerializer {
        public static Component fromJSON(String json) {
            return CraftChatMessage.fromJSON(json);
        }
        
        public static String toJSON(Component component) {
            return CraftChatMessage.toJSON(component);
        }
    }
}
