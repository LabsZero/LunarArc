package io.lunararcdevs.lunararc.common.mixin.core.events;

import io.lunararcdevs.lunararc.common.bridge.EntityBridge;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.FilteredText;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** ClientTickEndEvent and UncheckedSignChangeEvent, fired as the matching client packets arrive. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ExtraPacketEventsMixin {
    @Shadow public ServerPlayer player;

    @Inject(method = "handleClientTickEnd", at = @At("TAIL"), require = 0)
    private void lunararc$clientTickEnd(ServerboundClientTickEndPacket packet, CallbackInfo ci) {
        if (io.papermc.paper.event.packet.ClientTickEndEvent.getHandlerList().getRegisteredListeners().length == 0
                || !(((EntityBridge) this.player).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Player bukkit)) return;
        new io.papermc.paper.event.packet.ClientTickEndEvent(bukkit).callEvent();
    }

    @Inject(method = "updateSignText", at = @At("HEAD"), cancellable = true, require = 0)
    private void lunararc$uncheckedSignChange(ServerboundSignUpdatePacket packet, List<FilteredText> lines, CallbackInfo ci) {
        if (io.papermc.paper.event.packet.UncheckedSignChangeEvent.getHandlerList().getRegisteredListeners().length == 0) return;
        ServerLevel level = this.player.level();
        if (!level.hasChunkAt(packet.pos())
                || !(((EntityBridge) this.player).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Player bukkit)) return;
        List<Component> text = lines.stream().<Component>map(line -> Component.text(line.raw())).toList();
        io.papermc.paper.math.BlockPosition position = io.papermc.paper.math.Position.block(
                packet.pos().getX(), packet.pos().getY(), packet.pos().getZ());
        org.bukkit.block.sign.Side side = packet.slot() == net.minecraft.world.level.block.entity.SignTextSlot.FRONT ? org.bukkit.block.sign.Side.FRONT : org.bukkit.block.sign.Side.BACK;
        if (!new io.papermc.paper.event.packet.UncheckedSignChangeEvent(bukkit, position, side, text).callEvent()) ci.cancel();
    }
}
