package io.lunararcdevs.lunararc.common.mixin.core.events;

import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Opt-in (-Dlunararc.debugIngredients=true): encodes and decodes every custom payload a player is sent, with the server's own registries,
 * and logs the ones that cannot be read back (for example "Ingredient can't contain air"), naming the payload.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class IngredientPacketDebugMixin {
    @Shadow public ServerPlayer player;

    @Unique private static final Logger lunararc$log = LoggerFactory.getLogger("LunarArc");

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), require = 0)
    private void lunararc$checkPayload(Packet<?> packet, CallbackInfo ci) {
        if (!(packet instanceof ClientboundCustomPayloadPacket custom) || !Boolean.getBoolean("lunararc.debugIngredients")) return;
        String id = custom.payload().type().id().toString();
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), this.player.level().registryAccess());
        try {
            ClientboundCustomPayloadPacket.GAMEPLAY_STREAM_CODEC.encode(buf, custom);
            ClientboundCustomPayloadPacket.GAMEPLAY_STREAM_CODEC.decode(buf);
        } catch (Throwable error) {
            lunararc$log.warn("[ingredients] payload {} cannot be read back on the server: {}", id, error.toString());
            lunararc$log.warn("[ingredients] payload {} detail", id, error);
        } finally {
            buf.release();
        }
    }
}
