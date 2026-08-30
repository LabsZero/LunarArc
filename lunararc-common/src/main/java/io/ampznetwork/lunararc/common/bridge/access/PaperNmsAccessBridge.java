package io.ampznetwork.lunararc.common.bridge.access;

import io.ampznetwork.lunararc.common.bridge.ServerCommonPacketListenerBridge;
import net.minecraft.network.Connection;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;

/**
 * Narrow compatibility bridge for NMS members whose access is widened by Paper
 * but remains protected/private on the loader-owned Minecraft runtime.
 *
 * <p>Plugin bytecode is rewritten only for the exact widened member access. The
 * underlying Minecraft class remains owned and shaped by the active loader.</p>
 */
public final class PaperNmsAccessBridge {
    private PaperNmsAccessBridge() {
    }

    public static Connection serverCommonConnection(ServerCommonPacketListenerImpl listener) {
        return ((ServerCommonPacketListenerBridge) (Object) listener).lunararc$getConnection();
    }

    public static io.netty.channel.Channel connectionChannel(Connection connection) {
        return ((io.ampznetwork.lunararc.common.bridge.ConnectionBridge) connection).lunararc$getChannel();
    }
}
