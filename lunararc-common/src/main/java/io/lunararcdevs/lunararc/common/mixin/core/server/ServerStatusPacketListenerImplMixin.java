package io.lunararcdevs.lunararc.common.mixin.core.server;

import io.lunararcdevs.lunararc.common.server.LunarArcServerListPing;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.status.ServerboundStatusRequestPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerStatusPacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerStatusPacketListenerImpl.class)
public abstract class ServerStatusPacketListenerImplMixin {

    @Shadow @Final private Connection connection;

    @Inject(method = "handleStatusRequest", cancellable = true, require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;send(Lnet/minecraft/network/protocol/Packet;)V"))
    private void lunararc$serverListPing(ServerboundStatusRequestPacket packet, CallbackInfo ci) {
        MinecraftServer server = io.lunararcdevs.lunararc.common.LunarArcServerAccess.getMinecraftServer();
        if (server != null && LunarArcServerListPing.process(server, this.connection)) ci.cancel();
    }
}
