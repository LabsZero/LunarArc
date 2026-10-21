package io.lunararcdevs.lunararc.common.event;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.lunararcdevs.lunararc.common.LunarArcServerAccess;
import io.lunararcdevs.lunararc.common.bridge.EntityBridge;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Cancellable;

public final class LunarArcServerSideEvents {
    private LunarArcServerSideEvents() {
    }

    private static boolean listened(HandlerList handlers) {
        return handlers.getRegisteredListeners().length != 0;
    }

    private static boolean fire(Event event) {
        LunarArcServerAccess.getCraftServer().getPluginManager().callEvent(event);
        return event instanceof Cancellable cancellable && cancellable.isCancelled();
    }

    // RemoteServerCommandEvent -------------------------------------------------------------------------------------

    /** Returns the command to run, or null when a plugin cancelled it. */
    public static String remoteCommand(net.minecraft.commands.CommandSourceStack source, String command) {
        if (!listened(org.bukkit.event.server.RemoteServerCommandEvent.getHandlerList())) return command;
        org.bukkit.command.CommandSender sender;
        try {
            sender = ((io.lunararcdevs.lunararc.common.bridge.CommandSourceStackBridge) source).lunararc$getBukkitSender();
        } catch (Throwable unavailable) {
            sender = null;
        }
        if (sender == null) sender = LunarArcServerAccess.getCraftServer().getConsoleSender();
        org.bukkit.event.server.RemoteServerCommandEvent event = new org.bukkit.event.server.RemoteServerCommandEvent(sender, command);
        if (fire(event)) return null;
        return event.getCommand();
    }

    // ProfileWhitelistVerifyEvent ----------------------------------------------------------------------------------

    /**
     * @param vanillaDenial what vanilla decided (null when the profile may join)
     * @return the denial to use: unchanged, replaced by a plugin's message, or null when a plugin let the profile in
     */
    public static Component whitelistVerify(net.minecraft.server.players.PlayerList players, net.minecraft.server.players.NameAndId profile,
            Component vanillaDenial) {
        if (!listened(com.destroystokyo.paper.event.profile.ProfileWhitelistVerifyEvent.getHandlerList())) return vanillaDenial;
        boolean whitelistDenial = vanillaDenial != null && vanillaDenial.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents translatable
                && "multiplayer.disconnect.not_whitelisted".equals(translatable.getKey());
        // another check (ban, full server) already turned the profile away: leave that alone
        if (vanillaDenial != null && !whitelistDenial) return vanillaDenial;

        boolean op = players.isOp(profile);
        net.kyori.adventure.text.Component configured = io.lunararcdevs.lunararc.common.messaging.LunarArcComponentPipeline.toAdventure(
                whitelistDenial ? vanillaDenial : Component.translatable("multiplayer.disconnect.not_whitelisted"));
        com.destroystokyo.paper.event.profile.ProfileWhitelistVerifyEvent event = new com.destroystokyo.paper.event.profile.ProfileWhitelistVerifyEvent(
                new com.destroystokyo.paper.profile.CraftPlayerProfile(profile), players.isUsingWhitelist(), !whitelistDenial, op, configured);
        fire(event);
        if (event.isWhitelisted()) return whitelistDenial ? null : vanillaDenial;
        net.kyori.adventure.text.Component message = event.kickMessage() == null ? configured : event.kickMessage();
        return io.lunararcdevs.lunararc.common.messaging.LunarArcComponentPipeline.fromAdventure(message);
    }

    // AsyncChatDecorateEvent / AsyncChatCommandDecorateEvent ---------------------------------------------------------

    private static final ThreadLocal<Boolean> COMMAND_DECORATION = new ThreadLocal<>();

    /** Runs {@code decoration} flagged as command decoration, so the decorate event becomes AsyncChatCommandDecorateEvent. */
    public static Component asCommandDecoration(java.util.function.Supplier<Component> decoration) {
        Boolean previous = COMMAND_DECORATION.get();
        COMMAND_DECORATION.set(Boolean.TRUE);
        try {
            return decoration.get();
        } finally {
            if (previous == null) COMMAND_DECORATION.remove();
            else COMMAND_DECORATION.set(previous);
        }
    }

    public static Component decorateChat(ServerPlayer sender, Component original, boolean command) {
        command |= Boolean.TRUE.equals(COMMAND_DECORATION.get());
        final boolean asCommand = command;
        boolean decorate = listened(io.papermc.paper.event.player.AsyncChatDecorateEvent.getHandlerList())
                || listened(io.papermc.paper.event.player.AsyncChatCommandDecorateEvent.getHandlerList());
        if (!decorate) return original;
        Runnable work = null;
        Component[] result = {original};
        net.kyori.adventure.text.Component initial = io.lunararcdevs.lunararc.common.messaging.LunarArcComponentPipeline.toAdventure(original);
        org.bukkit.entity.Player player = sender == null ? null
                : ((EntityBridge) sender).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Player found ? found : null;
        Runnable task = () -> {
            io.papermc.paper.event.player.AsyncChatDecorateEvent event = asCommand
                    ? new io.papermc.paper.event.player.AsyncChatCommandDecorateEvent(player, initial)
                    : new io.papermc.paper.event.player.AsyncChatDecorateEvent(player, initial);
            if (!fire(event)) {
                result[0] = io.lunararcdevs.lunararc.common.messaging.LunarArcComponentPipeline.fromAdventure(event.result());
            }
        };
        // these events are asynchronous by contract, so a plugin must never see them on the server thread
        if (org.bukkit.Bukkit.isPrimaryThread()) CompletableFuture.runAsync(task).join();
        else task.run();
        return result[0];
    }

    // EntityPortalExitEvent ------------------------------------------------------------------------------------------

    public static TeleportTransition portalExit(Entity entity, TeleportTransition transition) {
        if (!listened(org.bukkit.event.entity.EntityPortalExitEvent.getHandlerList()) || transition == null || entity.isRemoved()
                || !(entity.level() instanceof net.minecraft.server.level.ServerLevel)) return transition;
        if (!(((EntityBridge) entity).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Entity bukkitEntity)) return transition;

        PositionMoveRotation absolute = PositionMoveRotation.calculateAbsolute(PositionMoveRotation.of(entity),
                PositionMoveRotation.of(transition), transition.relatives());
        Vec3 velocity = absolute.deltaMovement();
        org.bukkit.World world = LunarArcServerAccess.getCraftWorld(transition.newLevel());
        Location to = new Location(world, absolute.position().x, absolute.position().y, absolute.position().z, absolute.yRot(), absolute.xRot());
        org.bukkit.util.Vector before = bukkitEntity.getVelocity();
        org.bukkit.util.Vector after = new org.bukkit.util.Vector(velocity.x, velocity.y, velocity.z);

        org.bukkit.event.entity.EntityPortalExitEvent event = new org.bukkit.event.entity.EntityPortalExitEvent(
                bukkitEntity, bukkitEntity.getLocation(), to.clone(), before, after);
        fire(event);
        // only touch the transition when a plugin changed something, since that also drops its relative flags
        if (event.isCancelled() || !to.equals(event.getTo()) || !after.equals(event.getAfter())) {
            Location target;
            Vec3 newVelocity;
            if (event.isCancelled() || event.getTo() == null) {
                // cancelling does not cancel the teleport, only the position and velocity change (an old quirk Paper keeps)
                target = event.getFrom().clone();
                newVelocity = new Vec3(event.getBefore().getX(), event.getBefore().getY(), event.getBefore().getZ());
            } else {
                target = event.getTo().clone();
                newVelocity = new Vec3(event.getAfter().getX(), event.getAfter().getY(), event.getAfter().getZ());
            }
            return new TeleportTransition(transition.newLevel(), new Vec3(target.getX(), target.getY(), target.getZ()), newVelocity,
                    target.getYaw(), target.getPitch(), transition.missingRespawnBlock(), transition.asPassenger(), Set.of(),
                    transition.postTeleportTransition());
        }
        return transition;
    }

    // AsyncTabCompleteEvent / AsyncPlayerSendSuggestionsEvent ---------------------------------------------------------

    /**
     * Runs the plugin tab-complete events for a player's suggestion request.
     *
     * @param vanilla asks vanilla for its own suggestions when no plugin handled the request
     * @param send delivers the final suggestions to the client
     */
    public static void tabComplete(ServerPlayer player, String command, java.util.function.Supplier<CompletableFuture<Suggestions>> vanilla,
            java.util.function.Consumer<Suggestions> send) {
        org.bukkit.entity.Player bukkit = ((EntityBridge) player).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Player found ? found : null;
        if (bukkit == null) {
            vanilla.get().thenAccept(send);
            return;
        }
        CompletableFuture.runAsync(() -> {
            com.destroystokyo.paper.event.server.AsyncTabCompleteEvent event = new com.destroystokyo.paper.event.server.AsyncTabCompleteEvent(
                    bukkit, command, true, null);
            fire(event);
            List<com.destroystokyo.paper.event.server.AsyncTabCompleteEvent.Completion> completions = event.isCancelled() ? List.of() : event.completions();
            if (!event.isHandled()) {
                if (event.isCancelled()) return;
                vanilla.get().thenAccept(suggestions -> sendSuggestions(bukkit, command, suggestions, send));
            } else if (!completions.isEmpty()) {
                SuggestionsBuilder root = new SuggestionsBuilder(command, command.length());
                SuggestionsBuilder builder = root.createOffset(root.getInput().lastIndexOf(' ') + 1);
                for (com.destroystokyo.paper.event.server.AsyncTabCompleteEvent.Completion completion : completions) {
                    Component tooltip = completion.tooltip() == null ? null
                            : io.lunararcdevs.lunararc.common.messaging.LunarArcComponentPipeline.fromAdventure(completion.tooltip());
                    Integer number = com.google.common.primitives.Ints.tryParse(completion.suggestion());
                    if (number != null) builder.suggest(number, tooltip);
                    else builder.suggest(completion.suggestion(), tooltip);
                }
                sendSuggestions(bukkit, command, builder.buildFuture().join(), send);
            }
        });
    }

    private static void sendSuggestions(org.bukkit.entity.Player player, String command, Suggestions suggestions,
            java.util.function.Consumer<Suggestions> send) {
        if (!listened(com.destroystokyo.paper.event.brigadier.AsyncPlayerSendSuggestionsEvent.getHandlerList())) {
            send.accept(suggestions);
            return;
        }
        com.destroystokyo.paper.event.brigadier.AsyncPlayerSendSuggestionsEvent event =
                new com.destroystokyo.paper.event.brigadier.AsyncPlayerSendSuggestionsEvent(player, suggestions, command);
        // an empty list would make the client show "[<args>]" for commands with nothing more to offer
        if (!fire(event) && !event.getSuggestions().isEmpty()) send.accept(event.getSuggestions());
    }
}
