package org.bukkit.craftbukkit.v1_21_R1.event;

import com.mojang.datafixers.util.Pair;
import net.kyori.adventure.text.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.craftbukkit.v1_21_R1.block.CraftBlock;
import org.bukkit.craftbukkit.v1_21_R1.inventory.CraftItemStack;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;

public class CraftEventFactory {
    public static EntityDamageEvent callEntityDamageEvent(LivingEntity entity, DamageSource source, float damage) {
        DamageCause cause = DamageCause.CUSTOM;

        // Map Vanilla damage sources to Bukkit causes
        if (source.is(net.minecraft.world.damagesource.DamageTypes.FALL))
            cause = DamageCause.FALL;
        else if (source.is(net.minecraft.world.damagesource.DamageTypes.ON_FIRE))
            cause = DamageCause.FIRE_TICK;
        else if (source.is(net.minecraft.world.damagesource.DamageTypes.LAVA))
            cause = DamageCause.LAVA;
        else if (source.is(net.minecraft.world.damagesource.DamageTypes.DROWN))
            cause = DamageCause.DROWNING;
        else if (source.is(net.minecraft.world.damagesource.DamageTypes.STARVE))
            cause = DamageCause.STARVATION;
        else if (source.is(net.minecraft.world.damagesource.DamageTypes.MAGIC))
            cause = DamageCause.MAGIC;
        else if (source.is(net.minecraft.world.damagesource.DamageTypes.WITHER))
            cause = DamageCause.WITHER;

        org.bukkit.entity.Entity bukkitEntity = ((io.ampznetwork.lunararc.common.bridge.EntityBridge) entity)
                .lunararc$getBukkitEntity();
        @SuppressWarnings("deprecation")
        EntityDamageEvent event = new EntityDamageEvent(bukkitEntity, cause, (double) damage);
        Bukkit.getPluginManager().callEvent(event);
        return event;
    }

    public static PlayerInteractEvent callPlayerInteractEvent(net.minecraft.server.level.ServerPlayer player,
            Action action, @Nullable net.minecraft.core.BlockPos pos, @Nullable net.minecraft.core.Direction direction,
            @Nullable net.minecraft.world.item.ItemStack itemstack) {
        org.bukkit.entity.Player bukkitPlayer = (org.bukkit.entity.Player) ((io.ampznetwork.lunararc.common.bridge.EntityBridge) player)
                .lunararc$getBukkitEntity();
        if (bukkitPlayer == null)
            return null;

        ItemStack item = CraftItemStack.asBukkitCopy(itemstack);
        Block block = (pos != null) ? CraftBlock.create(player.serverLevel(), pos) : null;
        BlockFace face = (direction != null) ? BlockFace.valueOf(direction.name()) : BlockFace.SELF;

        PlayerInteractEvent event = new PlayerInteractEvent(bukkitPlayer, action, item, block, face);
        Bukkit.getPluginManager().callEvent(event);
        return event;
    }

    public static org.bukkit.event.block.BlockBreakEvent callBlockBreakEvent(
            net.minecraft.server.level.ServerLevel world, net.minecraft.core.BlockPos pos,
            net.minecraft.server.level.ServerPlayer player) {
        org.bukkit.block.Block block = org.bukkit.craftbukkit.v1_21_R1.block.CraftBlock.create(world, pos);
        org.bukkit.entity.Player bukkitPlayer = (org.bukkit.entity.Player) ((io.ampznetwork.lunararc.common.bridge.EntityBridge) player)
                .lunararc$getBukkitEntity();

        org.bukkit.event.block.BlockBreakEvent event = new org.bukkit.event.block.BlockBreakEvent(block, bukkitPlayer);
        Bukkit.getPluginManager().callEvent(event);
        return event;
    }

    public static org.bukkit.event.block.BlockPlaceEvent callBlockPlaceEvent(
            net.minecraft.server.level.ServerLevel world, net.minecraft.core.BlockPos pos,
            net.minecraft.server.level.ServerPlayer player, net.minecraft.world.InteractionHand hand,
            net.minecraft.world.level.block.state.BlockState newState) {
        org.bukkit.block.Block block = org.bukkit.craftbukkit.v1_21_R1.block.CraftBlock.create(world, pos);

        org.bukkit.entity.Player bukkitPlayer = player != null
                ? (org.bukkit.entity.Player) ((io.ampznetwork.lunararc.common.bridge.EntityBridge) player)
                        .lunararc$getBukkitEntity()
                : null;
        ItemStack item = player != null ? CraftItemStack.asBukkitCopy(player.getItemInHand(hand)) : null;

        org.bukkit.event.block.BlockPlaceEvent event = new org.bukkit.event.block.BlockPlaceEvent(block,
                block.getState(), block, item, bukkitPlayer, true, org.bukkit.inventory.EquipmentSlot.HAND);
        Bukkit.getPluginManager().callEvent(event);
        return event;
    }

    // Entity spawn event
    public static org.bukkit.event.entity.EntitySpawnEvent callEntitySpawnEvent(net.minecraft.world.entity.Entity entity) {
        org.bukkit.entity.Entity bukkitEntity = ((io.ampznetwork.lunararc.common.bridge.EntityBridge) entity).lunararc$getBukkitEntity();
        if (bukkitEntity == null) return null;
        var event = new org.bukkit.event.entity.EntitySpawnEvent(bukkitEntity);
        Bukkit.getPluginManager().callEvent(event);
        return event;
    }

    // Creature spawn event (for living entities)
    public static org.bukkit.event.entity.CreatureSpawnEvent callCreatureSpawnEvent(net.minecraft.world.entity.Mob entity, org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason reason) {
        org.bukkit.entity.Entity bukkitEntity = ((io.ampznetwork.lunararc.common.bridge.EntityBridge) entity).lunararc$getBukkitEntity();
        if (!(bukkitEntity instanceof org.bukkit.entity.LivingEntity living)) return null;
        var event = new org.bukkit.event.entity.CreatureSpawnEvent(living, reason);
        Bukkit.getPluginManager().callEvent(event);
        return event;
    }

    // Player join event
    public static org.bukkit.event.player.PlayerJoinEvent callPlayerJoinEvent(net.minecraft.server.level.ServerPlayer player) {
        org.bukkit.entity.Player bukkitPlayer = org.bukkit.Bukkit.getPlayer(player.getUUID());
        if (bukkitPlayer == null) return null;
        var event = new org.bukkit.event.player.PlayerJoinEvent(bukkitPlayer, net.kyori.adventure.text.Component.text(player.getScoreboardName() + " joined the game"));
        Bukkit.getPluginManager().callEvent(event);
        return event;
    }

    // Player quit event
    public static org.bukkit.event.player.PlayerQuitEvent callPlayerQuitEvent(net.minecraft.server.level.ServerPlayer player) {
        org.bukkit.entity.Player bukkitPlayer = org.bukkit.Bukkit.getPlayer(player.getUUID());
        if (bukkitPlayer == null) return null;
        var event = new org.bukkit.event.player.PlayerQuitEvent(bukkitPlayer, net.kyori.adventure.text.Component.text(player.getScoreboardName() + " left the game"));
        Bukkit.getPluginManager().callEvent(event);
        return event;
    }

    // Player move event
    public static org.bukkit.event.player.PlayerMoveEvent callPlayerMoveEvent(net.minecraft.server.level.ServerPlayer player, Location from, Location to) {
        org.bukkit.entity.Player bukkitPlayer = org.bukkit.Bukkit.getPlayer(player.getUUID());
        if (bukkitPlayer == null) return null;
        var event = new org.bukkit.event.player.PlayerMoveEvent(bukkitPlayer, from, to);
        Bukkit.getPluginManager().callEvent(event);
        return event;
    }

    // Player chat event — use AsyncPlayerChatEvent (legacy) to avoid adventure API coupling issues
    public static org.bukkit.event.player.AsyncPlayerChatEvent callAsyncChatEvent(
            net.minecraft.server.level.ServerPlayer player, String message) {
        org.bukkit.entity.Player bukkitPlayer = org.bukkit.Bukkit.getPlayer(player.getUUID());
        if (bukkitPlayer == null) return null;
        java.util.Set<org.bukkit.entity.Player> recipients = new java.util.HashSet<>(
                (java.util.Collection<? extends org.bukkit.entity.Player>)
                        org.bukkit.Bukkit.getOnlinePlayers());
        @SuppressWarnings("deprecation")
        var event = new org.bukkit.event.player.AsyncPlayerChatEvent(false, bukkitPlayer, message, recipients);
        Bukkit.getPluginManager().callEvent(event);
        return event;
    }

    // World load event
    public static void callWorldLoadEvent(org.bukkit.World world) {
        var event = new org.bukkit.event.world.WorldLoadEvent(world);
        Bukkit.getPluginManager().callEvent(event);
    }

    // World save event
    public static void callWorldSaveEvent(org.bukkit.World world) {
        var event = new org.bukkit.event.world.WorldSaveEvent(world);
        Bukkit.getPluginManager().callEvent(event);
    }

    // Entity death event
    @SuppressWarnings("deprecation")
    public static org.bukkit.event.entity.EntityDeathEvent callEntityDeathEvent(net.minecraft.world.entity.LivingEntity entity) {
        org.bukkit.entity.Entity bukkitEntity = ((io.ampznetwork.lunararc.common.bridge.EntityBridge) entity).lunararc$getBukkitEntity();
        if (!(bukkitEntity instanceof org.bukkit.entity.LivingEntity living)) return null;
        org.bukkit.damage.DamageSource deathSource = org.bukkit.damage.DamageSource
                .builder(org.bukkit.damage.DamageType.GENERIC).build();
        var event = new org.bukkit.event.entity.EntityDeathEvent(living, deathSource,
                new ArrayList<org.bukkit.inventory.ItemStack>());
        Bukkit.getPluginManager().callEvent(event);
        return event;
    }

    // Inventory open event. Mirrors Paper's CraftEventFactory semantics against the
    // live NMS container menu. The caller passes the already-built Bukkit view because
    // the vanilla AbstractContainerMenu has no getBukkitView() in LunarArc's runtime.
    public static AbstractContainerMenu callInventoryOpenEvent(ServerPlayer player,
            AbstractContainerMenu container, InventoryView view) {
        return callInventoryOpenEventWithTitle(player, container, view, false).getSecond();
    }

    public static AbstractContainerMenu callInventoryOpenEvent(ServerPlayer player,
            AbstractContainerMenu container, InventoryView view, boolean cancelled) {
        return callInventoryOpenEventWithTitle(player, container, view, cancelled).getSecond();
    }

    public static Pair<Component, AbstractContainerMenu> callInventoryOpenEventWithTitle(
            ServerPlayer player, AbstractContainerMenu container, InventoryView view, boolean cancelled) {
        InventoryOpenEvent event = new InventoryOpenEvent(view);
        event.setCancelled(cancelled);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return Pair.of(null, null);
        }
        return Pair.of(event.titleOverride(), container);
    }

    // Inventory close event. Only fires when a real menu (not the player's own
    // inventoryMenu) is open. Closing the native menu itself is left to the caller,
    // which keeps the various close paths (plugin, client, disconnect) distinct.
    public static void handleInventoryCloseEvent(ServerPlayer player) {
        handleInventoryCloseEvent(player, InventoryCloseEvent.Reason.UNKNOWN);
    }

    public static void handleInventoryCloseEvent(ServerPlayer player, InventoryCloseEvent.Reason reason) {
        if (player.containerMenu == player.inventoryMenu) return;
        Object bukkit = ((io.ampznetwork.lunararc.common.bridge.EntityBridge) player).lunararc$getBukkitEntity();
        if (!(bukkit instanceof HumanEntity human)) return;
        InventoryCloseEvent event = new InventoryCloseEvent(human.getOpenInventory(), reason);
        Bukkit.getPluginManager().callEvent(event);
    }
}
