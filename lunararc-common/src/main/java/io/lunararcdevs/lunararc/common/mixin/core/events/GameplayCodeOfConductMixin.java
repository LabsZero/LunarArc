package io.lunararcdevs.lunararc.common.mixin.core.events;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.lunararcdevs.lunararc.common.bridge.ConfigurationConnectionBridge;
import io.papermc.paper.event.connection.configuration.PlayerCodeOfConductSendEvent;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * PlayerCodeOfConductSendEvent: plugins may replace the code of conduct a client is shown. The event only runs on servers that
 * configure a code of conduct; a plugin clearing it keeps the configured one rather than sending nothing.
 */
@Mixin(ServerConfigurationPacketListenerImpl.class)
public abstract class GameplayCodeOfConductMixin {
    @ModifyReturnValue(method = "lambda$addOptionalTasks$0", at = @At("RETURN"), require = 1)
    private String lunararc$codeOfConduct(String original) {
        if (PlayerCodeOfConductSendEvent.getHandlerList().getRegisteredListeners().length == 0) return original;
        io.papermc.paper.connection.PaperPlayerConfigurationConnection connection =
                ((ConfigurationConnectionBridge) (Object) this).lunararc$paperConnection();
        if (connection == null) return original;
        PlayerCodeOfConductSendEvent event = new PlayerCodeOfConductSendEvent(connection, original);
        event.callEvent();
        return event.getCodeOfConduct() == null ? original : event.getCodeOfConduct();
    }
}
