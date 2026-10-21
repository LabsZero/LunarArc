package io.lunararcdevs.lunararc.common.mixin.core.server;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.lunararcdevs.lunararc.common.event.LunarArcServerSideEvents;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.ChatDecorator;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Marks decoration done on behalf of a command (/say, /msg, ...) so plugins get AsyncChatCommandDecorateEvent instead of AsyncChatDecorateEvent. */
@Mixin(MessageArgument.class)
public abstract class ServerSideCommandDecorateMixin {
    @WrapOperation(method = {"resolveSignedMessage", "resolveDisguisedMessage"}, require = 2,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/ChatDecorator;decorate(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/network/chat/Component;)Lnet/minecraft/network/chat/Component;"))
    private static Component lunararc$commandDecorate(ChatDecorator decorator, ServerPlayer sender, Component message, Operation<Component> original) {
        return LunarArcServerSideEvents.asCommandDecoration(() -> original.call(decorator, sender, message));
    }
}
