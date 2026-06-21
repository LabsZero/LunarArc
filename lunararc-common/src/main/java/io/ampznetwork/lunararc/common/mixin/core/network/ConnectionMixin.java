package io.ampznetwork.lunararc.common.mixin.core.network;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public abstract class ConnectionMixin {

    @Shadow public Channel channel;

    @Unique public Channel n;

    @Inject(method = "channelActive", at = @At("TAIL"))
    private void lunararc$syncNField(ChannelHandlerContext ctx, CallbackInfo ci) {
        n = channel;
    }
}
