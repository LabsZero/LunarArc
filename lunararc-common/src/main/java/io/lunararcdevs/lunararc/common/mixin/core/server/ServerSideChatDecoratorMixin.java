package io.lunararcdevs.lunararc.common.mixin.core.server;

import io.lunararcdevs.lunararc.common.event.LunarArcServerSideEvents;
import net.minecraft.network.chat.ChatDecorator;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** AsyncChatDecorateEvent: every message vanilla decorates (chat, /say, /msg, ...) passes through plugins first. */
@Mixin(MinecraftServer.class)
public abstract class ServerSideChatDecoratorMixin {
    @Inject(method = "getChatDecorator", at = @At("RETURN"), cancellable = true, require = 0)
    private void lunararc$decorator(CallbackInfoReturnable<ChatDecorator> cir) {
        ChatDecorator vanilla = cir.getReturnValue();
        cir.setReturnValue((sender, message) -> LunarArcServerSideEvents.decorateChat(sender, vanilla.decorate(sender, message), false));
    }
}
