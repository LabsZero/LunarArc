package io.lunararcdevs.lunararc.common.compat;

import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;

public final class LunarArcPlayerInfoUpdatePacketCompat {
    private static final Field ENTRIES_FIELD = findField("entries");
    private static final ThreadLocal<ClientboundPlayerInfoUpdatePacket.Entry> PENDING = new ThreadLocal<>();

    private LunarArcPlayerInfoUpdatePacketCompat() {
    }

    public static Collection<ServerPlayer> stash(ClientboundPlayerInfoUpdatePacket.Entry entry) {
        PENDING.set(entry);
        return List.of();
    }

    public static ClientboundPlayerInfoUpdatePacket apply(ClientboundPlayerInfoUpdatePacket packet) {
        ClientboundPlayerInfoUpdatePacket.Entry entry = PENDING.get();
        PENDING.remove();
        try {
            ENTRIES_FIELD.set(packet, List.of(entry));
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not construct single-entry ClientboundPlayerInfoUpdatePacket", e);
        }
        return packet;
    }

    private static Field findField(String name) {
        try {
            Field field = io.lunararcdevs.lunararc.common.mod.LunarArcReflectionBridge.getDeclaredField(
                    ClientboundPlayerInfoUpdatePacket.class, name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}
