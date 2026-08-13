package io.ampznetwork.lunararc.common.mixin.core.server;

import io.ampznetwork.lunararc.common.LunarArcPlatform;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {

    @Shadow
    public ServerPlayer player;

    @Inject(method = "handleChat", at = @At("HEAD"), cancellable = true)
    private void lunararc$onChat(ServerboundChatPacket packet, CallbackInfo ci) {
        try {
            Object craftServer = LunarArcPlatform.getServer();
            if (craftServer == null)
                return;

            Class<?> eventClass = Class.forName("org.bukkit.event.player.AsyncPlayerChatEvent");
            Object bukkitPlayer = ((io.ampznetwork.lunararc.common.bridge.EntityBridge) this.player).lunararc$getBukkitEntity();
            String message = packet.message();

            java.util.Set<?> recipients = new java.util.HashSet<>(
                    (java.util.Collection<?>) craftServer.getClass().getMethod("getOnlinePlayers").invoke(craftServer));

            // AsyncPlayerChatEvent(boolean async, Player who, String message, Set<Player>
            // players)
            Object event = eventClass
                    .getConstructor(boolean.class, Class.forName("org.bukkit.entity.Player"), String.class,
                            java.util.Set.class)
                    .newInstance(false, bukkitPlayer, message, recipients);

            Object pm = craftServer.getClass().getMethod("getPluginManager").invoke(craftServer);
            pm.getClass().getMethod("callEvent", Class.forName("org.bukkit.event.Event")).invoke(pm, event);

            Boolean isCancelled = (Boolean) eventClass.getMethod("isCancelled").invoke(event);
            if (isCancelled) {
                ci.cancel();
            }
        } catch (Throwable ignored) {
        }
    }

    @Inject(method = "handleChatCommand", at = @At("HEAD"), cancellable = true)
    private void lunararc$onChatCommand(ServerboundChatCommandPacket packet, CallbackInfo ci) {
        org.bukkit.Server craftServer = LunarArcPlatform.getServer();
        if (craftServer == null) return;

        // Network packets arrive on a Netty event-loop thread. Paper/Bukkit command
        // preprocessing is synchronous-only, so never fire PlayerCommandPreprocessEvent
        // from here until Minecraft has handed the packet to the main server thread.
        // Re-entering handleChatCommand on that thread also preserves Minecraft's native
        // command/signing path when LunarArcCommandRouter returns PASS.
        if (craftServer instanceof org.bukkit.craftbukkit.v1_21_R1.CraftServer lunarArcServer) {
            net.minecraft.server.MinecraftServer minecraftServer = lunarArcServer.getHandle();
            if (!minecraftServer.isSameThread()) {
                minecraftServer.execute(() ->
                        ((ServerGamePacketListenerImpl) (Object) this).handleChatCommand(packet));
                ci.cancel();
                return;
            }
        }

        Object bukkit = ((io.ampznetwork.lunararc.common.bridge.EntityBridge) this.player)
                .lunararc$getBukkitEntity();
        if (!(bukkit instanceof org.bukkit.entity.Player bukkitPlayer)) return;

        io.ampznetwork.lunararc.common.server.LunarArcCommandRouter.PacketResult result =
                io.ampznetwork.lunararc.common.server.LunarArcCommandRouter.routePlayerPacket(
                        craftServer, bukkitPlayer, packet.command());
        if (result == io.ampznetwork.lunararc.common.server.LunarArcCommandRouter.PacketResult.CANCEL) {
            ci.cancel();
        }
    }

    // Fire InventoryCloseEvent(Reason.PLAYER) when the client closes its own screen.
    // The vanilla handler still performs the actual close, so this only fires the
    // Bukkit event; the menu id guard skips stale close packets from previous menus.
    @Inject(method = "handleContainerClose", at = @At("HEAD"))
    private void lunararc$onContainerClose(ServerboundContainerClosePacket packet, CallbackInfo ci) {
        try {
            if (this.player.containerMenu == this.player.inventoryMenu) return;
            if (this.player.containerMenu.containerId != packet.getContainerId()) return;
            Object bukkit = ((io.ampznetwork.lunararc.common.bridge.EntityBridge) this.player).lunararc$getBukkitEntity();
            if (bukkit instanceof org.bukkit.entity.Player) {
                org.bukkit.craftbukkit.v1_21_R1.event.CraftEventFactory.handleInventoryCloseEvent(
                        this.player, org.bukkit.event.inventory.InventoryCloseEvent.Reason.PLAYER);
            }
        } catch (Throwable ignored) {
        }
    }

}
