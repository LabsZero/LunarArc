package io.lunararcdevs.lunararc.common.mixin.core.server;

import com.mojang.authlib.GameProfile;
import io.lunararcdevs.lunararc.common.LunarArcServerAccess;
import io.lunararcdevs.lunararc.common.bridge.ConnectionBridge;
import io.lunararcdevs.lunararc.common.bridge.EntityBridge;
import io.lunararcdevs.lunararc.common.bridge.PlayerListBridge;
import io.lunararcdevs.lunararc.common.bridge.ServerLoginPacketListenerBridge;
import io.lunararcdevs.lunararc.common.bridge.ServerPlayerBukkitDataBridge;
import io.lunararcdevs.lunararc.common.messaging.LunarArcComponentPipeline;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.minecraft.server.players.IpBanList;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.UserBanList;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin implements PlayerListBridge {

    @Unique private net.minecraft.world.level.portal.TeleportTransition lunararc$lastRespawnTransition;
    @Unique private boolean lunararc$lastRespawnBedSpawn;
    @Unique private boolean lunararc$lastRespawnAnchor;
    @Unique private boolean lunararc$lastRespawnMissing;
    @Unique private org.bukkit.event.player.PlayerRespawnEvent.RespawnReason lunararc$lastRespawnReason = org.bukkit.event.player.PlayerRespawnEvent.RespawnReason.DEATH;

    @Shadow @Final private MinecraftServer server;
    @Shadow @Final private UserBanList bans;
    @Shadow @Final private IpBanList ipBans;
    @Shadow @Final public List<ServerPlayer> players;

    @Shadow public abstract boolean isWhiteListed(net.minecraft.server.players.NameAndId profile);
    @Shadow public abstract boolean canBypassPlayerLimit(net.minecraft.server.players.NameAndId profile);
    @Shadow public abstract Component canPlayerLogin(SocketAddress address, net.minecraft.server.players.NameAndId profile);
    @Shadow public abstract void broadcastSystemMessage(Component component, boolean overlay);

    @org.spongepowered.asm.mixin.injection.ModifyVariable(
            method = "broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private Component lunararc$rewriteAdvancementAnnouncement(Component message) {
        return io.lunararcdevs.lunararc.common.event.LunarArcAdvancementEvents.rewrite(message);
    }

    @Inject(method = "broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void lunararc$suppressAdvancementAnnouncement(Component message, boolean overlay, CallbackInfo ci) {
        if (io.lunararcdevs.lunararc.common.event.LunarArcAdvancementEvents.suppressed()) ci.cancel();
    }


    @Override
    public CraftServer lunararc$getCraftServer() {
        return LunarArcServerAccess.getCraftServer(this.server);
    }

    @Override
    public void lunararc$reloadRecipeData() {
        net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket packet =
                new net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket(
                        this.server.getRecipeManager().getSynchronizedItemProperties(),
                        this.server.getRecipeManager().getSynchronizedStonecutterRecipes());
        for (ServerPlayer player : this.players) {
            if (player.connection != null && player.connection.isAcceptingMessages()) {
                player.connection.send(packet);
            }
        }
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void lunararc$attachCraftServer(CallbackInfo ci) {
        io.lunararcdevs.lunararc.common.bridge.MinecraftServerBridge bridge =
                (io.lunararcdevs.lunararc.common.bridge.MinecraftServerBridge) this.server;
        if (bridge.lunararc$getCraftServer() == null) {
            bridge.lunararc$setCraftServer(new CraftServer(this.server, (PlayerList) (Object) this));
        }
    }

    @Override
    public ServerPlayer lunararc$canPlayerLogin(SocketAddress address, GameProfile profile, ServerLoginPacketListenerImpl handler) {
        Component vanillaDenial = this.canPlayerLogin(address, new net.minecraft.server.players.NameAndId(profile));
        ServerPlayer player = new ServerPlayer(this.server, this.server.overworld(), profile, ClientInformation.createDefault());
        Player bukkitPlayer = (Player) ((EntityBridge) player).lunararc$getBukkitEntity();

        Connection connection = ((ServerLoginPacketListenerBridge) handler).lunararc$getConnection();
        ConnectionBridge connectionBridge = (ConnectionBridge) connection;
        InetAddress forwardedAddress = address instanceof InetSocketAddress inet ? inet.getAddress() : null;
        SocketAddress rawAddress = connectionBridge.lunararc$getRawAddress();
        InetAddress realAddress = rawAddress instanceof InetSocketAddress inet ? inet.getAddress() : forwardedAddress;

        if (forwardedAddress == null || realAddress == null) {
            throw new IllegalStateException("Player login connection does not expose an InetAddress");
        }

        PlayerLoginEvent event = new PlayerLoginEvent(
                bukkitPlayer,
                connectionBridge.lunararc$getHostname(),
                forwardedAddress,
                realAddress);

        if (vanillaDenial != null) {
            event.disallow(this.lunararc$loginResult(address, profile),
                    LunarArcComponentPipeline.toAdventure(vanillaDenial));
        }

        this.lunararc$getCraftServer().getPluginManager().callEvent(event);
        if (event.getResult() != PlayerLoginEvent.Result.ALLOWED) {
            ((ServerLoginPacketListenerBridge) handler)
                    .lunararc$disconnect(LunarArcComponentPipeline.fromAdventure(event.kickMessage()));
            return null;
        }

        connectionBridge.lunararc$setLoginPlayer(player);
        return player;
    }

    @Unique
    private PlayerLoginEvent.Result lunararc$loginResult(SocketAddress address, GameProfile profile) {
        if (this.bans.isBanned(new net.minecraft.server.players.NameAndId(profile))) {
            return PlayerLoginEvent.Result.KICK_BANNED;
        }
        if (!this.isWhiteListed(new net.minecraft.server.players.NameAndId(profile))) {
            return PlayerLoginEvent.Result.KICK_WHITELIST;
        }
        if (this.ipBans.isBanned(address)) {
            return PlayerLoginEvent.Result.KICK_BANNED;
        }
        if (this.players.size() >= this.server.getMaxPlayers() && !this.canBypassPlayerLimit(new net.minecraft.server.players.NameAndId(profile))) {
            return PlayerLoginEvent.Result.KICK_FULL;
        }
        return PlayerLoginEvent.Result.KICK_OTHER;
    }

    @Inject(method = "placeNewPlayer", at = @At("HEAD"))
    private void lunararc$beginJoin(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) {
        ((ServerPlayerBukkitDataBridge) player).lunararc$setLoginTime(System.currentTimeMillis());
    }

    @Redirect(
            method = "respawn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;findRespawnPositionAndUseSpawnBlock(ZLnet/minecraft/world/level/portal/TeleportTransition$PostTeleportTransition;)Lnet/minecraft/world/level/portal/TeleportTransition;"),
            require = 0)
    private net.minecraft.world.level.portal.TeleportTransition lunararc$respawnEvent(
            ServerPlayer player,
            boolean keepEverything,
            net.minecraft.world.level.portal.TeleportTransition.PostTeleportTransition postTransition) {
        net.minecraft.world.level.portal.TeleportTransition vanilla =
                ((io.lunararcdevs.lunararc.common.bridge.ServerPlayerRespawnBridge) (Object) player).lunararc$findRespawnPositionAndUseSpawnBlock(keepEverything, postTransition);
        if (vanilla == null) {
            return null;
        }

        Player bukkitPlayer = (Player) ((EntityBridge) player).lunararc$getBukkitEntity();
        org.bukkit.craftbukkit.CraftWorld craftWorld =
                this.lunararc$getCraftServer().getCraftWorld(vanilla.newLevel());
        org.bukkit.Location location = new org.bukkit.Location(
                craftWorld,
                vanilla.position().x,
                vanilla.position().y,
                vanilla.position().z,
                vanilla.yRot(),
                vanilla.xRot());

        boolean bedSpawn = false;
        boolean anchorSpawn = false;
        net.minecraft.server.level.ServerPlayer.RespawnConfig respawnConfig = player.getRespawnConfig();
        if (respawnConfig != null && vanilla.newLevel() == this.server.getLevel(respawnConfig.respawnData().dimension())) {
            net.minecraft.core.BlockPos respawnPos = respawnConfig.respawnData().pos();
            net.minecraft.world.level.block.state.BlockState state = vanilla.newLevel().getBlockState(respawnPos);
            bedSpawn = state.getBlock() instanceof net.minecraft.world.level.block.BedBlock;
            anchorSpawn = state.is(net.minecraft.world.level.block.Blocks.RESPAWN_ANCHOR);
        }

        org.bukkit.event.player.PlayerRespawnEvent.RespawnReason reason = keepEverything
                ? org.bukkit.event.player.PlayerRespawnEvent.RespawnReason.END_PORTAL
                : org.bukkit.event.player.PlayerRespawnEvent.RespawnReason.DEATH;
        org.bukkit.event.player.PlayerRespawnEvent event =
                new org.bukkit.event.player.PlayerRespawnEvent(
                        bukkitPlayer, location, bedSpawn, anchorSpawn, vanilla.missingRespawnBlock(), reason);
        this.lunararc$lastRespawnReason = reason;
        this.lunararc$lastRespawnAnchor = anchorSpawn;
        this.lunararc$lastRespawnMissing = vanilla.missingRespawnBlock();
        this.lunararc$getCraftServer().getPluginManager().callEvent(event);

        org.bukkit.Location chosen = event.getRespawnLocation();
        if (!(chosen.getWorld() instanceof org.bukkit.craftbukkit.CraftWorld chosenWorld)) {
            net.minecraft.core.BlockPos vanillaPos = net.minecraft.core.BlockPos.containing(vanilla.position());
            net.minecraft.world.level.block.state.BlockState vanillaState = vanilla.newLevel().getBlockState(vanillaPos);
            this.lunararc$lastRespawnTransition = vanilla;
            this.lunararc$lastRespawnBedSpawn = vanillaState.is(net.minecraft.tags.BlockTags.BEDS)
                    && !vanilla.missingRespawnBlock();
            return vanilla;
        }
        net.minecraft.server.level.ServerLevel chosenLevel = chosenWorld.getHandle();
        net.minecraft.world.phys.Vec3 chosenPos = new net.minecraft.world.phys.Vec3(
                chosen.getX(), chosen.getY(), chosen.getZ());
        boolean unchanged = chosenLevel == vanilla.newLevel()
                && chosenPos.equals(vanilla.position())
                && chosen.getYaw() == vanilla.yRot()
                && chosen.getPitch() == vanilla.xRot();
        net.minecraft.world.level.portal.TeleportTransition selected = unchanged
                ? vanilla
                : new net.minecraft.world.level.portal.TeleportTransition(
                        chosenLevel,
                        chosenPos,
                        vanilla.deltaMovement(),
                        chosen.getYaw(),
                        chosen.getPitch(),
                        vanilla.missingRespawnBlock(),
                        vanilla.asPassenger(),
                        vanilla.relatives(),
                        vanilla.postTeleportTransition());

        net.minecraft.core.BlockPos selectedPos = net.minecraft.core.BlockPos.containing(selected.position());
        net.minecraft.world.level.block.state.BlockState selectedState = selected.newLevel().getBlockState(selectedPos);
        this.lunararc$lastRespawnTransition = selected;
        this.lunararc$lastRespawnBedSpawn = selectedState.is(net.minecraft.tags.BlockTags.BEDS)
                && !selected.missingRespawnBlock();
        return selected;
    }

    @Inject(method = "respawn", at = @At("RETURN"), require = 0)
    private void lunararc$postRespawn(ServerPlayer original, boolean keepEverything, net.minecraft.world.entity.Entity.RemovalReason removalReason, CallbackInfoReturnable<ServerPlayer> cir) {
        ServerPlayer respawned = cir.getReturnValue();
        net.minecraft.world.level.portal.TeleportTransition transition = this.lunararc$lastRespawnTransition;
        boolean bedSpawn = this.lunararc$lastRespawnBedSpawn;
        boolean anchorSpawn = this.lunararc$lastRespawnAnchor;
        boolean missingRespawn = this.lunararc$lastRespawnMissing;
        org.bukkit.event.player.PlayerRespawnEvent.RespawnReason respawnReason = this.lunararc$lastRespawnReason;
        this.lunararc$lastRespawnTransition = null;
        this.lunararc$lastRespawnBedSpawn = false;
        this.lunararc$lastRespawnAnchor = false;
        this.lunararc$lastRespawnMissing = false;
        if (respawned == null || transition == null || respawned.connection == null) return;

        if (original instanceof io.lunararcdevs.lunararc.common.bridge.ServerPlayerDeathBridge deathBridge) {
            var inventoryState = deathBridge.lunararc$getDeathInventoryState();
            if (inventoryState != null && !keepEverything) {
                respawned.getInventory().clearContent();
                int limit = Math.min(respawned.getInventory().getContainerSize(), inventoryState.size());
                for (int slot = 0; slot < limit; slot++) {
                    net.minecraft.world.item.ItemStack stack = inventoryState.get(slot);
                    respawned.getInventory().setItem(slot, stack == null ? net.minecraft.world.item.ItemStack.EMPTY : stack.copy());
                }
                deathBridge.lunararc$setDeathInventoryState(null);
            }
            var exp = deathBridge.lunararc$getDeathExperienceState();
            if (exp != null && !keepEverything) {
                if (exp.keepLevel()) {
                    respawned.experienceLevel = exp.oldLevel();
                    respawned.totalExperience = exp.oldTotalExperience();
                    respawned.experienceProgress = exp.oldProgress();
                } else {
                    respawned.experienceLevel = Math.max(0, exp.newLevel());
                    respawned.totalExperience = Math.max(0, exp.newTotalExperience());
                    respawned.setExperiencePoints(Math.max(0, exp.newExp()));
                }
                respawned.connection.send(new net.minecraft.network.protocol.game.ClientboundSetExperiencePacket(
                        respawned.experienceProgress, respawned.totalExperience, respawned.experienceLevel));
                deathBridge.lunararc$setDeathExperienceState(null);
            }
        }

        Object bukkit = ((EntityBridge) respawned).lunararc$getBukkitEntity();
        if (!(bukkit instanceof org.bukkit.craftbukkit.entity.CraftPlayer craftPlayer)) return;

        // Paper/Spigot refresh these values after ClientboundRespawnPacket because a
        // respawn may also move the player to a world with different settings.
        respawned.connection.send(new net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket(
                craftPlayer.getSendViewDistance()));
        respawned.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSimulationDistancePacket(
                craftPlayer.getSimulationDistance()));
        craftPlayer.sendHealthUpdate();

        // Resend the authoritative entity metadata after the new player instance is live.
        java.util.List<net.minecraft.network.syncher.SynchedEntityData.DataValue<?>> values =
                respawned.getEntityData().getNonDefaultValues();
        if (values != null) {
            respawned.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket(
                    respawned.getId(), values));
        }

        org.bukkit.Location postLocation = new org.bukkit.Location(
                craftPlayer.getWorld(), respawned.getX(), respawned.getY(), respawned.getZ(),
                respawned.getYRot(), respawned.getXRot());
        this.lunararc$getCraftServer().getPluginManager().callEvent(
                new com.destroystokyo.paper.event.player.PlayerPostRespawnEvent(
                        craftPlayer, postLocation, bedSpawn, anchorSpawn, missingRespawn, respawnReason));
    }

    @Redirect(
            method = "placeNewPlayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    private void lunararc$playerJoinEvent(
            PlayerList instance,
            Component vanillaMessage,
            boolean overlay,
            Connection connection,
            ServerPlayer player,
            CommonListenerCookie cookie) {
        Player bukkitPlayer = (Player) ((EntityBridge) player).lunararc$getBukkitEntity();
        boolean addedForEvent = !this.players.contains(player);
        if (addedForEvent) {
            this.players.add(player);
        }

        PlayerJoinEvent event = new PlayerJoinEvent(bukkitPlayer, LunarArcComponentPipeline.toAdventure(vanillaMessage));
        try {
            this.lunararc$getCraftServer().getPluginManager().callEvent(event);
        } finally {
            if (addedForEvent) {
                this.players.remove(player);
            }
        }

        net.kyori.adventure.text.Component message = event.joinMessage();
        if (message != null && !message.equals(net.kyori.adventure.text.Component.empty())) {
            this.broadcastSystemMessage(LunarArcComponentPipeline.fromAdventure(message), overlay);
        }

        io.lunararcdevs.lunararc.common.events.PlayerJoinListener.checkAndNotify(
                bukkitPlayer,
                player.getGameProfile(),
                this.server,
                this.server::execute);
    }

    @Inject(
            method = "placeNewPlayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V",
                    shift = At.Shift.AFTER),
            cancellable = true)
    private void lunararc$cancelJoinIfDisconnected(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) {
        if (player.connection != null && !player.connection.isAcceptingMessages()) {
            ci.cancel();
        }
    }

    @Inject(method = "remove", at = @At("HEAD"))
    private void lunararc$playerQuitEvent(ServerPlayer player, CallbackInfo ci) {
        io.lunararcdevs.lunararc.common.server.LunarArcContext.clear();

        if (player.containerMenu != player.inventoryMenu) {
            // Let the real ServerPlayer close path fire InventoryCloseEvent exactly once.
            ((io.lunararcdevs.lunararc.common.bridge.ServerPlayerInventoryBridge) player)
                    .lunararc$setNextInventoryCloseReason(
                            org.bukkit.event.inventory.InventoryCloseEvent.Reason.DISCONNECT);
        }

        Player bukkitPlayer = (Player) ((EntityBridge) player).lunararc$getBukkitEntity();
        net.kyori.adventure.text.Component message = net.kyori.adventure.text.Component.translatable(
                "multiplayer.player.left",
                net.kyori.adventure.text.format.NamedTextColor.YELLOW,
                bukkitPlayer.displayName());
        PlayerQuitEvent event = new PlayerQuitEvent(
                bukkitPlayer,
                message,
                PlayerQuitEvent.QuitReason.DISCONNECTED);
        this.lunararc$getCraftServer().getPluginManager().callEvent(event);
    }


}
