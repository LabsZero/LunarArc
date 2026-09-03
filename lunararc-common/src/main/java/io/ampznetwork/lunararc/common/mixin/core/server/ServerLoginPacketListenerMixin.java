package io.ampznetwork.lunararc.common.mixin.core.server;

import com.mojang.authlib.GameProfile;
import io.ampznetwork.lunararc.common.bridge.MinecraftServerBridge;
import io.ampznetwork.lunararc.common.bridge.PlayerListBridge;
import io.ampznetwork.lunararc.common.bridge.ServerLoginPacketListenerBridge;
import io.ampznetwork.lunararc.common.config.LunarArcConfig;
import io.ampznetwork.lunararc.common.messaging.LunarArcComponentPipeline;
import io.ampznetwork.lunararc.common.mod.util.VelocitySupport;
import net.minecraft.Util;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.login.ClientboundCustomQueryPacket;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerPreLoginEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

@Mixin(ServerLoginPacketListenerImpl.class)
public abstract class ServerLoginPacketListenerMixin implements ServerLoginPacketListenerBridge {

    @Shadow @Final MinecraftServer server;
    @Shadow private GameProfile authenticatedProfile;
    @Shadow public Connection connection;
    @Shadow public abstract void disconnect(Component reason);
    @Shadow abstract void startClientVerification(GameProfile profile);

    @Unique private static final Logger lunararc$logger = LoggerFactory.getLogger("LunarArc");
    @Unique private int lunararc$velocityLoginId = -1;
    @Unique private volatile boolean lunararc$preLoginCompleted;
    @Unique private ServerPlayer lunararc$loginPlayer;

    @Override
    public Connection lunararc$getConnection() {
        return this.connection;
    }

    @Override
    public void lunararc$disconnect(Component reason) {
        this.disconnect(reason);
    }

    @Inject(method = "handleHello", at = @At("HEAD"), cancellable = true)
    private void lunararc$velocityHello(ServerboundHelloPacket packet, CallbackInfo ci) {
        if (!LunarArcConfig.isVelocityEnabled()) {
            return;
        }

        this.lunararc$velocityLoginId = ThreadLocalRandom.current().nextInt();
        this.connection.send(new ClientboundCustomQueryPacket(
                this.lunararc$velocityLoginId,
                (net.minecraft.network.protocol.login.custom.CustomQueryPayload) VelocitySupport.createPacket()));
        ci.cancel();
    }

    @Inject(
            method = "handleCustomQueryPacket",
            cancellable = true,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/network/ServerLoginPacketListenerImpl;disconnect(Lnet/minecraft/network/chat/Component;)V"))
    private void lunararc$velocityResponse(ServerboundCustomQueryAnswerPacket packet, CallbackInfo ci) {
        if (!LunarArcConfig.isVelocityEnabled() || packet.transactionId() != this.lunararc$velocityLoginId) {
            return;
        }

        var payload = packet.payload();
        if (payload == null) {
            this.disconnect(Component.literal("This server requires you to connect with Velocity."));
            ci.cancel();
            return;
        }

        FriendlyByteBuf buf = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        payload.write(buf);

        if (!VelocitySupport.checkIntegrity(buf, LunarArcConfig.getVelocitySecret())) {
            this.disconnect(Component.literal("Unable to verify player details."));
            ci.cancel();
            return;
        }

        int version = buf.readVarInt();
        if (version > VelocitySupport.MAX_SUPPORTED_FORWARDING_VERSION) {
            throw new IllegalStateException("Unsupported Velocity forwarding version " + version);
        }

        int port = this.connection.getRemoteAddress() instanceof InetSocketAddress address ? address.getPort() : 0;
        this.connection.address = new InetSocketAddress(VelocitySupport.readAddress(buf), port);
        this.authenticatedProfile = VelocitySupport.createProfile(buf);

        Util.backgroundExecutor().execute(() -> {
            try {
                this.lunararc$preLogin(this.authenticatedProfile);
            } catch (Exception exception) {
                this.disconnect(Component.translatable("multiplayer.disconnect.unverified_username"));
                lunararc$logger.error("Login processing failed for {}", this.authenticatedProfile.getName(), exception);
            }
        });
        ci.cancel();
    }

    /**
     * Fire Bukkit's pre-login events for {@code gameprofile}, under CraftBukkit's own name.
     *
     * <p>CraftBukkit declares {@code callPlayerPreLoginEvents} on this class and calls it from
     * both login paths; Paper widens it to return the profile the events settled on. Plugins that
     * inject a login of their own drive that pair reflectively rather than reimplementing the
     * event contract: Floodgate's SpigotDataHandler builds a GameProfile for a Bedrock player,
     * then invokes {@code callPlayerPreLoginEvents(GameProfile)} followed by
     * {@code startClientVerification(GameProfile)} - and its ClassNames initializer asserts both
     * exist before Floodgate will enable at all. Keeping the events behind a
     * {@code lunararc}-prefixed name left that lookup empty, so this carries the real one.</p>
     *
     * <p>Declared public where CraftBukkit has it private, because a private method added by a
     * mixin is not guaranteed to keep its name through merging. Nothing reads the modifier -
     * Floodgate calls {@code setAccessible} either way - so the wider one is the safe choice.</p>
     *
     * <p>The profile comes back unchanged. Paper replaces it with whatever a listener left on the
     * event's PlayerProfile, which needs CraftPlayerProfile from Paper's server internals; until
     * LunarArc carries that, returning the input matches CraftBukkit's own behaviour rather than
     * pretending to honour a mutation that never happened.</p>
     */
    public GameProfile callPlayerPreLoginEvents(GameProfile gameprofile) throws Exception {
        String playerName = gameprofile.getName();
        SocketAddress remoteAddress = this.connection.getRemoteAddress();
        if (!(remoteAddress instanceof InetSocketAddress inetAddress)) {
            throw new IllegalStateException("Login connection does not expose an InetSocketAddress");
        }

        InetAddress address = inetAddress.getAddress();
        AsyncPlayerPreLoginEvent asyncEvent =
                new AsyncPlayerPreLoginEvent(playerName, address, gameprofile.getId());
        org.bukkit.Bukkit.getPluginManager().callEvent(asyncEvent);

        if (PlayerPreLoginEvent.getHandlerList().getRegisteredListeners().length != 0) {
            PlayerPreLoginEvent syncEvent =
                    new PlayerPreLoginEvent(playerName, address, gameprofile.getId());

            if (asyncEvent.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
                syncEvent.disallow(
                        PlayerPreLoginEvent.Result.valueOf(asyncEvent.getLoginResult().name()),
                        asyncEvent.kickMessage());
            }

            if (this.server.isSameThread()) {
                org.bukkit.Bukkit.getPluginManager().callEvent(syncEvent);
            } else {
                CompletableFuture<Void> result = new CompletableFuture<>();
                ((MinecraftServerBridge) this.server).lunararc$queueTask(() -> {
                    try {
                        org.bukkit.Bukkit.getPluginManager().callEvent(syncEvent);
                        result.complete(null);
                    } catch (Throwable throwable) {
                        result.completeExceptionally(throwable);
                    }
                });
                result.join();
            }

            if (syncEvent.getResult() != PlayerPreLoginEvent.Result.ALLOWED) {
                this.disconnect(LunarArcComponentPipeline.fromAdventure(syncEvent.kickMessage()));
            }
        } else if (asyncEvent.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            this.disconnect(LunarArcComponentPipeline.fromAdventure(asyncEvent.kickMessage()));
        }

        return gameprofile;
    }

    /**
     * Kick with a legacy-formatted string, the overload CraftBukkit adds beside vanilla's
     * Component one.
     *
     * <p>Floodgate rejects a Bedrock login through it ({@code LOGIN_DISCONNECT}), and asserts it
     * exists during class initialization. The body follows Paper's rather than CraftBukkit's:
     * section-legacy deserialization instead of {@code Component.literal}, so a kick message
     * carrying hex colours arrives coloured.</p>
     */
    public void disconnect(String reason) {
        this.disconnect(LunarArcComponentPipeline.fromAdventure(
                net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection()
                        .deserialize(reason)));
    }

    @Override
    public void lunararc$preLogin(GameProfile profile) throws Exception {
        GameProfile resolved = this.callPlayerPreLoginEvents(profile);

        // callPlayerPreLoginEvents disconnects on a denied result but still returns, the way
        // Paper's does. Paper guards the follow-up work with the same connection check rather
        // than an early return, so a kicked login stops here without a second code path.
        ((MinecraftServerBridge) this.server).lunararc$queueTask(() -> {
            if (!this.connection.isConnected()) return;
            this.lunararc$preLoginCompleted = true;
            this.startClientVerification(resolved);
        });
    }

    @Inject(method = "startClientVerification", at = @At("HEAD"), cancellable = true)
    private void lunararc$preLoginBeforeVerification(GameProfile profile, CallbackInfo ci) {
        if (this.lunararc$preLoginCompleted) {
            return;
        }

        Util.backgroundExecutor().execute(() -> {
            try {
                this.lunararc$preLogin(profile);
            } catch (Exception exception) {
                this.disconnect(Component.translatable("multiplayer.disconnect.unverified_username"));
                lunararc$logger.error("Pre-login processing failed for {}", profile.getName(), exception);
            }
        });
        ci.cancel();
    }

    @Redirect(
            method = "verifyLoginAndFinishConnectionSetup",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/players/PlayerList;canPlayerLogin(Ljava/net/SocketAddress;Lcom/mojang/authlib/GameProfile;)Lnet/minecraft/network/chat/Component;"))
    private Component lunararc$playerLoginEvent(PlayerList playerList, SocketAddress address, GameProfile profile) {
        if (this.lunararc$loginPlayer == null) {
            this.lunararc$loginPlayer = ((PlayerListBridge) playerList)
                    .lunararc$canPlayerLogin(address, profile, (ServerLoginPacketListenerImpl) (Object) this);
        }
        return null;
    }

    @Inject(
            method = "verifyLoginAndFinishConnectionSetup",
            cancellable = true,
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/players/PlayerList;canPlayerLogin(Ljava/net/SocketAddress;Lcom/mojang/authlib/GameProfile;)Lnet/minecraft/network/chat/Component;",
                    shift = At.Shift.AFTER))
    private void lunararc$stopDeniedLogin(GameProfile profile, CallbackInfo ci) {
        if (this.lunararc$loginPlayer == null) {
            ci.cancel();
        }
    }
}
