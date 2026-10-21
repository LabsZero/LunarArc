package io.lunararcdevs.lunararc.common.mixin.core.server;

import io.lunararcdevs.lunararc.common.event.LunarArcServerSideEvents;
import java.net.SocketAddress;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** ProfileWhitelistVerifyEvent: plugins may admit or turn away a profile, with their own message, after vanilla's whitelist check. */
@Mixin(PlayerList.class)
public abstract class ServerSideWhitelistVerifyMixin {
    @Inject(method = "canPlayerLogin(Ljava/net/SocketAddress;Lnet/minecraft/server/players/NameAndId;)Lnet/minecraft/network/chat/Component;",
            at = @At("RETURN"), cancellable = true, require = 0)
    private void lunararc$verifyWhitelist(SocketAddress address, NameAndId profile, CallbackInfoReturnable<Component> cir) {
        Component result = LunarArcServerSideEvents.whitelistVerify((PlayerList) (Object) this, profile, cir.getReturnValue());
        if (result != cir.getReturnValue()) cir.setReturnValue(result);
    }
}
