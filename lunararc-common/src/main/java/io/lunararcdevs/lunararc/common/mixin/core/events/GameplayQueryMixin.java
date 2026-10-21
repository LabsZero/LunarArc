package io.lunararcdevs.lunararc.common.mixin.core.events;

import com.destroystokyo.paper.event.server.GS4QueryEvent;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import java.net.DatagramPacket;
import java.net.InetAddress;
import net.minecraft.server.ServerInterface;
import net.minecraft.server.rcon.thread.QueryThreadGs4;
import org.bukkit.Bukkit;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * GS4QueryEvent: a UDP query is answered from the values a plugin left in the event. The motd, map, player counts, port, host name and
 * game version are honoured for both the basic and the full response; the full response keeps vanilla's plugin and player lists.
 */
@Mixin(QueryThreadGs4.class)
public abstract class GameplayQueryMixin {
    @Shadow @Final private String serverName;
    @Shadow @Final private String worldName;
    @Shadow @Final private int maxPlayers;
    @Shadow @Final private int serverPort;
    @Shadow private String hostIp;
    @Shadow @Final private ServerInterface serverInterface;

    @Unique private GS4QueryEvent.QueryResponse lunararc$response;
    @Unique private GS4QueryEvent.QueryType lunararc$type;
    @Unique private InetAddress lunararc$address;

    @Inject(method = "processPacket", at = @At("HEAD"), require = 1)
    private void lunararc$beginBasic(DatagramPacket packet, CallbackInfoReturnable<Boolean> cir) {
        this.lunararc$response = null;
        this.lunararc$type = GS4QueryEvent.QueryType.BASIC;
        this.lunararc$address = packet.getAddress();
    }

    @Inject(method = "buildRuleResponse", at = @At("HEAD"), require = 1)
    private void lunararc$beginFull(DatagramPacket packet, CallbackInfoReturnable<byte[]> cir) {
        this.lunararc$response = null;
        this.lunararc$type = GS4QueryEvent.QueryType.FULL;
        this.lunararc$address = packet.getAddress();
    }

    @Unique
    private GS4QueryEvent.QueryResponse lunararc$response() {
        if (this.lunararc$response == null) {
            GS4QueryEvent.QueryResponse initial = GS4QueryEvent.QueryResponse.builder()
                    .motd(this.serverName).map(this.worldName).currentPlayers(this.serverInterface.getPlayerCount())
                    .maxPlayers(this.maxPlayers).port(this.serverPort).hostname(this.hostIp)
                    .gameVersion(this.serverInterface.getServerVersion())
                    .serverVersion(Bukkit.getName() + " on " + Bukkit.getBukkitVersion()).build();
            if (GS4QueryEvent.getHandlerList().getRegisteredListeners().length == 0) {
                this.lunararc$response = initial;
            } else {
                GS4QueryEvent event = new GS4QueryEvent(this.lunararc$type, this.lunararc$address, initial);
                Bukkit.getPluginManager().callEvent(event);
                this.lunararc$response = event.getResponse();
            }
        }
        return this.lunararc$response;
    }

    // basic response ---------------------------------------------------------------------------------------------

    @ModifyExpressionValue(method = "processPacket", require = 1,
            at = @At(value = "FIELD", target = "Lnet/minecraft/server/rcon/thread/QueryThreadGs4;serverName:Ljava/lang/String;", opcode = Opcodes.GETFIELD, ordinal = 0))
    private String lunararc$basicMotd(String original) {
        return this.lunararc$response().getMotd();
    }

    @ModifyExpressionValue(method = "processPacket", require = 1,
            at = @At(value = "FIELD", target = "Lnet/minecraft/server/rcon/thread/QueryThreadGs4;worldName:Ljava/lang/String;", opcode = Opcodes.GETFIELD, ordinal = 0))
    private String lunararc$basicMap(String original) {
        return this.lunararc$response().getMap();
    }

    @ModifyExpressionValue(method = "processPacket", require = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/ServerInterface;getPlayerCount()I", ordinal = 0))
    private int lunararc$basicPlayers(int original) {
        return this.lunararc$response().getCurrentPlayers();
    }

    @ModifyExpressionValue(method = "processPacket", require = 1,
            at = @At(value = "FIELD", target = "Lnet/minecraft/server/rcon/thread/QueryThreadGs4;maxPlayers:I", opcode = Opcodes.GETFIELD, ordinal = 0))
    private int lunararc$basicMax(int original) {
        return this.lunararc$response().getMaxPlayers();
    }

    @ModifyExpressionValue(method = "processPacket", require = 1,
            at = @At(value = "FIELD", target = "Lnet/minecraft/server/rcon/thread/QueryThreadGs4;serverPort:I", opcode = Opcodes.GETFIELD, ordinal = 0))
    private int lunararc$basicPort(int original) {
        return this.lunararc$response().getPort();
    }

    @ModifyExpressionValue(method = "processPacket", require = 1,
            at = @At(value = "FIELD", target = "Lnet/minecraft/server/rcon/thread/QueryThreadGs4;hostIp:Ljava/lang/String;", opcode = Opcodes.GETFIELD, ordinal = 0))
    private String lunararc$basicHost(String original) {
        return this.lunararc$response().getHostname();
    }

    // full response ----------------------------------------------------------------------------------------------

    @ModifyExpressionValue(method = "buildRuleResponse", require = 1,
            at = @At(value = "FIELD", target = "Lnet/minecraft/server/rcon/thread/QueryThreadGs4;serverName:Ljava/lang/String;", opcode = Opcodes.GETFIELD, ordinal = 0))
    private String lunararc$fullMotd(String original) {
        return this.lunararc$response().getMotd();
    }

    @ModifyExpressionValue(method = "buildRuleResponse", require = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/ServerInterface;getServerVersion()Ljava/lang/String;", ordinal = 0))
    private String lunararc$fullVersion(String original) {
        return this.lunararc$response().getGameVersion();
    }

    @ModifyExpressionValue(method = "buildRuleResponse", require = 1,
            at = @At(value = "FIELD", target = "Lnet/minecraft/server/rcon/thread/QueryThreadGs4;worldName:Ljava/lang/String;", opcode = Opcodes.GETFIELD, ordinal = 0))
    private String lunararc$fullMap(String original) {
        return this.lunararc$response().getMap();
    }

    @ModifyExpressionValue(method = "buildRuleResponse", require = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/ServerInterface;getPlayerCount()I", ordinal = 0))
    private int lunararc$fullPlayers(int original) {
        return this.lunararc$response().getCurrentPlayers();
    }

    @ModifyExpressionValue(method = "buildRuleResponse", require = 1,
            at = @At(value = "FIELD", target = "Lnet/minecraft/server/rcon/thread/QueryThreadGs4;maxPlayers:I", opcode = Opcodes.GETFIELD, ordinal = 0))
    private int lunararc$fullMax(int original) {
        return this.lunararc$response().getMaxPlayers();
    }

    @ModifyExpressionValue(method = "buildRuleResponse", require = 1,
            at = @At(value = "FIELD", target = "Lnet/minecraft/server/rcon/thread/QueryThreadGs4;serverPort:I", opcode = Opcodes.GETFIELD, ordinal = 0))
    private int lunararc$fullPort(int original) {
        return this.lunararc$response().getPort();
    }

    @ModifyExpressionValue(method = "buildRuleResponse", require = 1,
            at = @At(value = "FIELD", target = "Lnet/minecraft/server/rcon/thread/QueryThreadGs4;hostIp:Ljava/lang/String;", opcode = Opcodes.GETFIELD, ordinal = 0))
    private String lunararc$fullHost(String original) {
        return this.lunararc$response().getHostname();
    }
}
