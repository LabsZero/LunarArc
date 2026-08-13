package org.bukkit.craftbukkit.v1_21_R1.entity;

import net.minecraft.server.level.ServerPlayer;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.entity.Entity;
import org.bukkit.craftbukkit.v1_21_R1.CraftServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.UUID;
import java.util.Set;
import java.util.Collections;
import java.net.InetSocketAddress;
import java.net.InetAddress;
import net.md_5.bungee.api.chat.BaseComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.map.MapView;
import org.bukkit.NamespacedKey;
import org.bukkit.Statistic;
import org.bukkit.Material;
import org.bukkit.GameMode;
import io.ampznetwork.lunararc.common.bridge.EntityBridge;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Collection;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.function.Consumer;
import org.bukkit.util.Vector;
import org.bukkit.entity.Projectile;
import org.bukkit.damage.DamageSource;
import net.kyori.adventure.util.TriState;
import org.bukkit.util.RayTraceResult;
import org.bukkit.entity.Item;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.FluidCollisionMode;
import org.bukkit.entity.FishHook;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.MainHand;
import org.bukkit.inventory.Merchant;
import org.bukkit.craftbukkit.v1_21_R1.inventory.CraftItemStack;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Firework;
import org.bukkit.entity.EntityCategory;
import org.bukkit.entity.memory.MemoryKey;
import io.papermc.paper.entity.Frictional;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.entity.TargetEntityInfo;

@SuppressWarnings("all")
public class CraftPlayer extends CraftHumanEntity implements Player {
    // Removed redundant player field to fix initialization order NPEs

    private final org.bukkit.craftbukkit.v1_21_R1.inventory.CraftPlayerInventory inventory;
    private final org.bukkit.craftbukkit.v1_21_R1.inventory.CraftNMSInventory enderChest;

    public CraftPlayer(CraftServer server, ServerPlayer player) {
        super(server, player);
        // Paper exposes stable live inventory wrappers; do not manufacture a
        // detached wrapper on every API call.
        this.inventory = new org.bukkit.craftbukkit.v1_21_R1.inventory.CraftPlayerInventory(player.getInventory(), this);
        this.enderChest = new org.bukkit.craftbukkit.v1_21_R1.inventory.CraftNMSInventory(player.getEnderChestInventory(), this, org.bukkit.event.inventory.InventoryType.ENDER_CHEST);
    }

    private net.kyori.adventure.text.Component tabListHeader = net.kyori.adventure.text.Component.empty();
    private net.kyori.adventure.text.Component tabListFooter = net.kyori.adventure.text.Component.empty();
    private String displayName;
    private Location compassTarget;
    private org.bukkit.scoreboard.Scoreboard scoreboard;
    private long playerTimeOffset;
    private boolean playerTimeRelative = true;
    private org.bukkit.WeatherType playerWeather;
    private TriState flyingFallDamage = TriState.NOT_SET;
    private boolean sleepingIgnored;
    private boolean hasSeenWinScreen;
    private Location respawnLocation;
    private boolean whitelisted;

    // Per-viewer visibility/listing state. Paper treats tab-list visibility separately
    // from entity visibility, so keep the two concerns independent.
    private final Set<UUID> unlistedPlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> legacyHiddenEntities = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Set<Plugin>> pluginHiddenEntities = new ConcurrentHashMap<>();

    public ServerPlayer getHandle() {
        return (ServerPlayer) entity;
    }

    @Override
    public org.bukkit.entity.EntityType getType() { return org.bukkit.entity.EntityType.PLAYER; }

    @Override
    public String getName() {
        return getHandle().getScoreboardName();
    }

    @Override
    public @NotNull net.kyori.adventure.text.Component name() {
        return net.kyori.adventure.text.Component.text(getName());
    }

    private final org.bukkit.entity.Player.Spigot spigot = new org.bukkit.entity.Player.Spigot() {
        @SuppressWarnings("deprecation")
        @Override
        public void sendMessage(net.md_5.bungee.api.chat.BaseComponent component) {
            sendBungeeComponents(component == null ? new net.md_5.bungee.api.chat.BaseComponent[0]
                    : new net.md_5.bungee.api.chat.BaseComponent[] { component });
        }

        @SuppressWarnings("deprecation")
        @Override
        public void sendMessage(net.md_5.bungee.api.chat.BaseComponent... components) {
            sendBungeeComponents(components);
        }

        @SuppressWarnings("deprecation")
        @Override
        public void sendMessage(@Nullable UUID sender, net.md_5.bungee.api.chat.BaseComponent component) {
            sendMessage(component);
        }

        @SuppressWarnings("deprecation")
        @Override
        public void sendMessage(@Nullable UUID sender, net.md_5.bungee.api.chat.BaseComponent... components) {
            sendMessage(components);
        }

        @SuppressWarnings("deprecation")
        @Override
        public void sendMessage(net.md_5.bungee.api.ChatMessageType position,
                net.md_5.bungee.api.chat.BaseComponent... components) {
            if (position == net.md_5.bungee.api.ChatMessageType.ACTION_BAR) {
                sendBungeeActionBar(components);
            } else {
                sendBungeeComponents(components);
            }
        }
    };

    @SuppressWarnings("deprecation")
    @Override
    public org.bukkit.entity.Player.Spigot spigot() {
        return spigot;
    }

    private void sendBungeeComponents(net.md_5.bungee.api.chat.BaseComponent... components) {
        io.ampznetwork.lunararc.common.messaging.LunarArcComponentPipeline.sendSystem(getHandle(), components);
    }

    private void sendBungeeActionBar(net.md_5.bungee.api.chat.BaseComponent... components) {
        io.ampznetwork.lunararc.common.messaging.LunarArcComponentPipeline.sendActionBar(getHandle(), components);
    }

    @Override
    public void setWalkSpeed(float value) {
        validateSpeed(value);
        getHandle().getAbilities().setWalkingSpeed(value / 2.0F);
        syncAbilities();
    }

    @Override
    public float getWalkSpeed() {
        return getHandle().getAbilities().getWalkingSpeed() * 2.0F;
    }

    @Override
    public void setFlySpeed(float value) {
        validateSpeed(value);
        getHandle().getAbilities().setFlyingSpeed(value / 2.0F);
        syncAbilities();
    }

    @Override
    public float getFlySpeed() {
        return getHandle().getAbilities().getFlyingSpeed() * 2.0F;
    }

    @Override
    public void setAllowFlight(boolean flight) {
        getHandle().getAbilities().mayfly = flight;
        if (!flight && getHandle().getAbilities().flying) {
            getHandle().getAbilities().flying = false;
        }
        syncAbilities();
    }

    @Override
    public boolean getAllowFlight() {
        return getHandle().getAbilities().mayfly;
    }

    @Override
    public void setFlying(boolean value) {
        if (value && !getAllowFlight()) {
            throw new IllegalArgumentException("Cannot make player fly if getAllowFlight() is false");
        }
        if (getHandle().getAbilities().flying == value) {
            return;
        }
        getHandle().getAbilities().flying = value;
        syncAbilities();
    }

    @Override
    public boolean isFlying() {
        return getHandle().getAbilities().flying;
    }

    @Override
    public void setFlyingFallDamage(@NotNull TriState flyingFallDamage) {
        if (flyingFallDamage == null) throw new IllegalArgumentException("flyingFallDamage cannot be null");
        this.flyingFallDamage = flyingFallDamage;
    }

    @Override
    public @NotNull TriState hasFlyingFallDamage() { return flyingFallDamage; }

    @Override
    public boolean isSprinting() { return getHandle().isSprinting(); }

    @Override
    public void setSprinting(boolean sprinting) { getHandle().setSprinting(sprinting); }

    @Override
    public void setSleepingIgnored(boolean isSleeping) { this.sleepingIgnored = isSleeping; }

    @Override
    public boolean isSleepingIgnored() { return sleepingIgnored; }

    @Override
    public boolean isTransferred() { return false; }

    @Override
    public @Nullable InetSocketAddress getHAProxyAddress() { return null; }

    @Override
    public boolean hasSeenWinScreen() { return hasSeenWinScreen; }

    @Override
    public void setHasSeenWinScreen(boolean hasSeenWinScreen) { this.hasSeenWinScreen = hasSeenWinScreen; }

    @Override
    public void showWinScreen() {}

    @Override
    public void saveData() {
        try {
            ((io.ampznetwork.lunararc.common.mixin.core.server.PlayerListAccessor)
                getHandle().server.getPlayerList()).lunararc$save(getHandle());
        } catch (Throwable ignored) {}
    }

    @Override
    public void loadData() {}

    @Override
    public boolean performCommand(@NotNull String command) {
        if (command == null) throw new IllegalArgumentException("command cannot be null");
        String normalized = command.startsWith("/") ? command.substring(1) : command;
        return getServer().dispatchCommand(this, normalized);
    }

    @Override
    public void chat(@NotNull String msg) {
        if (msg == null) throw new IllegalArgumentException("msg cannot be null");
        if (msg.startsWith("/")) performCommand(msg.substring(1)); else sendMessage(msg);
    }

    @Override
    public @NotNull CompletableFuture<byte[]> retrieveCookie(@NotNull NamespacedKey key) {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void storeCookie(@NotNull NamespacedKey key, byte @NotNull [] value) {}

    @Override
    public void transfer(@NotNull String host, int port) {
        if (host == null || host.isBlank()) throw new IllegalArgumentException("host cannot be blank");
        if (port < 0 || port > 65535) throw new IllegalArgumentException("port out of range");
        try { getHandle().connection.send(new net.minecraft.network.protocol.common.ClientboundTransferPacket(host, port)); } catch (Throwable ignored) {}
    }

    @Override
    public void kick() { kick(net.kyori.adventure.text.Component.text("Kicked by server")); }

    @Override
    public void kick(@Nullable net.kyori.adventure.text.Component message) {
        kick(message, org.bukkit.event.player.PlayerKickEvent.Cause.UNKNOWN);
    }

    @Override
    public void kick(@Nullable net.kyori.adventure.text.Component message, @NotNull org.bukkit.event.player.PlayerKickEvent.Cause cause) {
        if (getHandle().connection != null) {
            getHandle().connection.disconnect(adventureToNms(message == null ? net.kyori.adventure.text.Component.text("Kicked by server") : message));
        }
    }

    @Override
    public void kickPlayer(@Nullable String message) {
        kick(message == null ? null : net.kyori.adventure.text.Component.text(message));
    }

    @Override
    public boolean isListed(@NotNull Player other) {
        if (other == null) throw new IllegalArgumentException("other player cannot be null");
        return !unlistedPlayers.contains(other.getUniqueId());
    }

    @Override
    public boolean unlistPlayer(@NotNull Player other) {
        if (other == null) throw new IllegalArgumentException("other player cannot be null");
        boolean wasListed = unlistedPlayers.add(other.getUniqueId());
        if (wasListed) {
            sendPlayerListVisibility(other, false);
        }
        return wasListed;
    }

    @Override
    public boolean listPlayer(@NotNull Player other) {
        if (other == null) throw new IllegalArgumentException("other player cannot be null");
        if (!canSee(other)) {
            throw new IllegalStateException("Cannot list a player that is hidden from this player");
        }
        boolean wasUnlisted = unlistedPlayers.remove(other.getUniqueId());
        if (wasUnlisted) {
            sendPlayerListVisibility(other, true);
        }
        return wasUnlisted;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void hidePlayer(@NotNull Player player) {
        if (player == null) throw new IllegalArgumentException("player cannot be null");
        legacyHiddenEntities.add(player.getUniqueId());
        unlistPlayer(player);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void showPlayer(@NotNull Player player) {
        if (player == null) throw new IllegalArgumentException("player cannot be null");
        legacyHiddenEntities.remove(player.getUniqueId());
        if (canSee(player)) listPlayer(player);
    }

    @Override
    public void hidePlayer(@NotNull Plugin plugin, @NotNull Player player) {
        hideEntity(plugin, player);
        unlistPlayer(player);
    }

    @Override
    public void showPlayer(@NotNull Plugin plugin, @NotNull Player player) {
        showEntity(plugin, player);
        if (canSee(player)) listPlayer(player);
    }

    @Override
    public boolean canSee(@NotNull Player player) {
        return canSee((Entity) player);
    }

    @Override
    public void hideEntity(@NotNull Plugin plugin, @NotNull Entity target) {
        if (plugin == null) throw new IllegalArgumentException("plugin cannot be null");
        if (target == null) throw new IllegalArgumentException("entity cannot be null");
        pluginHiddenEntities.computeIfAbsent(target.getUniqueId(), ignored -> ConcurrentHashMap.newKeySet()).add(plugin);
    }

    @Override
    public void showEntity(@NotNull Plugin plugin, @NotNull Entity target) {
        if (plugin == null) throw new IllegalArgumentException("plugin cannot be null");
        if (target == null) throw new IllegalArgumentException("entity cannot be null");
        Set<Plugin> plugins = pluginHiddenEntities.get(target.getUniqueId());
        if (plugins == null) return;
        plugins.remove(plugin);
        if (plugins.isEmpty()) pluginHiddenEntities.remove(target.getUniqueId(), plugins);
    }

    @Override
    public boolean canSee(@NotNull Entity target) {
        if (target == null) throw new IllegalArgumentException("entity cannot be null");
        UUID id = target.getUniqueId();
        if (legacyHiddenEntities.contains(id)) return false;
        Set<Plugin> plugins = pluginHiddenEntities.get(id);
        return plugins == null || plugins.isEmpty();
    }

    /**
     * Sends a per-viewer tab-list update without coupling LunarArc to an unstable
     * packet constructor signature. Minecraft/Paper changes the packet factory
     * shape between releases, so resolve the 1.21.x packet form reflectively.
     */
    private void sendPlayerListVisibility(Player other, boolean listed) {
        if (getHandle().connection == null || !(other instanceof CraftPlayer craftOther)) return;
        try {
            Object packet = null;
            if (!listed) {
                Class<?> removeClass = Class.forName("net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket");
                for (java.lang.reflect.Constructor<?> ctor : removeClass.getDeclaredConstructors()) {
                    Class<?>[] params = ctor.getParameterTypes();
                    if (params.length == 1 && java.util.List.class.isAssignableFrom(params[0])) {
                        ctor.setAccessible(true);
                        packet = ctor.newInstance(java.util.List.of(other.getUniqueId()));
                        break;
                    }
                }
            } else {
                Class<?> updateClass = Class.forName("net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket");
                for (java.lang.reflect.Method method : updateClass.getDeclaredMethods()) {
                    if (!java.lang.reflect.Modifier.isStatic(method.getModifiers())) continue;
                    if (!updateClass.isAssignableFrom(method.getReturnType())) continue;
                    Class<?>[] params = method.getParameterTypes();
                    if (params.length == 1 && java.util.Collection.class.isAssignableFrom(params[0])) {
                        method.setAccessible(true);
                        packet = method.invoke(null, java.util.List.of(craftOther.getHandle()));
                        if (packet != null) break;
                    }
                }
            }
            if (packet instanceof net.minecraft.network.protocol.Packet<?> nmsPacket) {
                getHandle().connection.send(nmsPacket);
            }
        } catch (Throwable ignored) {
            // State remains authoritative even if a loader changes the packet
            // implementation; a later full player-info sync will reconcile it.
        }
    }

    @Override
    public UUID getUniqueId() {
        return getHandle().getUUID();
    }

    @Override
    public InetSocketAddress getAddress() {
        if (getHandle().connection == null) return null;
        return (InetSocketAddress) getHandle().connection.getRemoteAddress();
    }

    @Override
    public void sendMessage(String message) {
        io.ampznetwork.lunararc.common.messaging.LunarArcComponentPipeline.sendSystem(getHandle(), message);
    }

    private void sendLegacyComponentDirect(String message) {
        io.ampznetwork.lunararc.common.messaging.LunarArcComponentPipeline.sendSystem(getHandle(), message);
    }

    @Override
    public void sendMessage(String... messages) {
        if (messages == null) return;
        for (String msg : messages) sendMessage(msg);
    }

    @Override
    public void sendMessage(net.kyori.adventure.text.Component message) {
        io.ampznetwork.lunararc.common.messaging.LunarArcComponentPipeline.sendSystem(getHandle(), message);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void sendMessage(net.kyori.adventure.identity.Identity identity,
            net.kyori.adventure.text.Component message, net.kyori.adventure.audience.MessageType type) {
        sendMessage(message);
    }

    private void syncAbilities() {
        getHandle().onUpdateAbilities();
    }

    private static void validateSpeed(float value) {
        if (!Float.isFinite(value) || value < -1.0f || value > 1.0f) {
            throw new IllegalArgumentException("Speed must be finite and between -1.0 and 1.0");
        }
    }

    private static net.minecraft.network.chat.Component adventureToNms(net.kyori.adventure.text.Component component) {
        return io.ampznetwork.lunararc.common.messaging.LunarArcComponentPipeline.fromAdventure(component);
    }

    private static net.kyori.adventure.text.Component bungeeToAdventure(net.md_5.bungee.api.chat.BaseComponent... components) {
        if (components == null || components.length == 0) return net.kyori.adventure.text.Component.empty();
        try {
            String json = net.md_5.bungee.chat.ComponentSerializer.toString(components);
            return net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson().deserialize(json);
        } catch (Throwable ignored) {
            return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(
                    net.md_5.bungee.api.chat.BaseComponent.toLegacyText(components));
        }
    }

    @Override
    public String getDisplayName() {
        return displayName == null ? getName() : displayName;
    }

    @Override
    public void setDisplayName(String name) { this.displayName = name; }

    @Override
    public @NotNull Component displayName() { return Component.text(getDisplayName()); }

    @Override
    public void displayName(@Nullable Component displayName) {
        this.displayName = displayName == null ? null : net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(displayName);
    }

    private void sendTabListPacket() {
        try {
            getHandle().connection.send(new net.minecraft.network.protocol.game.ClientboundTabListPacket(
                    adventureToNms(tabListHeader), adventureToNms(tabListFooter)));
        } catch (Throwable ignored) {}
    }

    @Override
    public void sendPlayerListHeader(@NotNull net.kyori.adventure.text.Component header) {
        tabListHeader = header;
        sendTabListPacket();
    }

    @Override
    public void sendPlayerListFooter(@NotNull net.kyori.adventure.text.Component footer) {
        tabListFooter = footer;
        sendTabListPacket();
    }

    @Override
    public void sendPlayerListHeaderAndFooter(@NotNull net.kyori.adventure.text.Component header, @NotNull net.kyori.adventure.text.Component footer) {
        this.tabListHeader = header;
        this.tabListFooter = footer;
        sendTabListPacket();
    }
    @Override public Component playerListHeader() { return tabListHeader; }
    @Override public Component playerListFooter() { return tabListFooter; }
    @Override public @Nullable String getPlayerListHeader() { return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(tabListHeader); }
    @Override public @Nullable String getPlayerListFooter() { return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(tabListFooter); }
    @Override public void setPlayerListHeader(String header) {
        tabListHeader = header == null ? net.kyori.adventure.text.Component.empty() : net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(header);
        sendTabListPacket();
    }
    @Override public void setPlayerListFooter(String footer) {
        tabListFooter = footer == null ? net.kyori.adventure.text.Component.empty() : net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(footer);
        sendTabListPacket();
    }
    @Override public void setPlayerListHeaderFooter(String header, String footer) {
        tabListHeader = header == null ? net.kyori.adventure.text.Component.empty() : net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(header);
        tabListFooter = footer == null ? net.kyori.adventure.text.Component.empty() : net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(footer);
        sendTabListPacket();
    }
    @SuppressWarnings("deprecation")
    @Override public void setPlayerListHeaderFooter(@Nullable BaseComponent[] header, @Nullable BaseComponent[] footer) {
        tabListHeader = header == null ? net.kyori.adventure.text.Component.empty() : bungeeToAdventure(header);
        tabListFooter = footer == null ? net.kyori.adventure.text.Component.empty() : bungeeToAdventure(footer);
        sendTabListPacket();
    }
    @SuppressWarnings("deprecation")
    @Override public void setPlayerListHeaderFooter(@Nullable BaseComponent header, @Nullable BaseComponent footer) {
        setPlayerListHeaderFooter(
            header == null ? null : new BaseComponent[] { header },
            footer == null ? null : new BaseComponent[] { footer }
        );
    }
    @Override public String getPlayerListName() {
        net.minecraft.network.chat.Component display = getHandle().getTabListDisplayName();
        return display != null ? display.getString() : getName();
    }
    @Override public void setPlayerListName(String name) {
        try {
            var field = net.minecraft.server.level.ServerPlayer.class.getDeclaredField("tabListDisplayName");
            field.setAccessible(true);
            field.set(getHandle(), name == null ? null : net.minecraft.network.chat.Component.literal(name));
        } catch (Throwable ignored) {}
    }
    @Override public @NotNull Component playerListName() { return Component.text(getPlayerListName()); }
    @Override public void playerListName(@Nullable Component name) {
        setPlayerListName(name == null ? null : net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(name));
    }
    @Override public void setCompassTarget(Location loc) {
        if (loc == null) throw new IllegalArgumentException("Compass target cannot be null");
        this.compassTarget = loc.clone();
        if (getHandle().connection != null && loc.getWorld() == getWorld()) {
            getHandle().connection.send(new net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket(
                    new net.minecraft.core.BlockPos(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()), loc.getYaw()));
        }
    }
    @Override public Firework fireworkBoost(ItemStack stack) { return null; }
    @Override public Location getCompassTarget() {
        if (compassTarget != null) return compassTarget.clone();
        net.minecraft.core.BlockPos pos = getHandle().serverLevel().getSharedSpawnPos();
        return new Location(getWorld(), pos.getX(), pos.getY(), pos.getZ());
    }
    @Override public Iterable<? extends BossBar> activeBossBars() { return java.util.Collections.emptyList(); }
    @Override public void sendExperienceChange(float progress) {
        getHandle().connection.send(new net.minecraft.network.protocol.game.ClientboundSetExperiencePacket(progress, getHandle().totalExperience, getHandle().experienceLevel));
    }
    @Override public void sendMap(MapView map) {}
    @Override public void sendRawMessage(String message) { sendMessage(message); }
    @Override public void sendBlockChange(Location loc, Material material, byte data) {}
    @Override public void sendBlockChange(Location loc, org.bukkit.block.data.BlockData block) {}
    @Override public void sendBlockChanges(Collection<org.bukkit.block.BlockState> states) {}
    @Override public void sendBlockChanges(Collection<org.bukkit.block.BlockState> states, boolean ignoreAir) {}
    @Override public void sendBlockDamage(Location loc, float progress) {}
    @Override public void sendBlockDamage(Location loc, float progress, Entity entity) {}
    @Override public void sendBlockDamage(Location loc, float progress, int entityId) {}
    @Override public void sendEquipmentChange(LivingEntity entity, EquipmentSlot slot, ItemStack stack) {}
    @Override public void sendEquipmentChange(LivingEntity entity, Map<EquipmentSlot, ItemStack> equipment) {}
    @Override public void sendSignChange(Location loc, String[] lines) {}
    @Override public void sendSignChange(Location loc, String[] lines, DyeColor dyeColor) {}
    @Override public void sendSignChange(Location loc, String[] lines, DyeColor dyeColor, boolean hasGlowingText) {}
    @Override public void sendPluginMessage(org.bukkit.plugin.Plugin plugin, String channel, byte[] message) {}
    @Override public void sendPotionEffectChange(LivingEntity entity, PotionEffect effect) {}
    @Override public void sendPotionEffectChangeRemove(LivingEntity entity, PotionEffectType type) {}
    @Override public void sendBlockUpdate(Location loc, org.bukkit.block.TileState state) {}
    @Override public void sendSignChange(Location loc, List<? extends net.kyori.adventure.text.Component> lines, DyeColor dyeColor, boolean hasGlowingText) {}
    @Override public void sendHealthUpdate(double health, int foodLevel, float saturation) {
        if (getHandle().connection != null) {
            getHandle().connection.send(new net.minecraft.network.protocol.game.ClientboundSetHealthPacket(
                    (float) health, foodLevel, saturation));
        }
    }
    @Override public void sendHealthUpdate() {
        sendHealthUpdate(getHealth(), getFoodLevel(), getSaturation());
    }
    @Override public void sendMultiBlockChange(Map<? extends io.papermc.paper.math.Position, org.bukkit.block.data.BlockData> blocks) {}
    @Override public void hideTitle() {
        try { getHandle().connection.send(new net.minecraft.network.protocol.game.ClientboundClearTitlesPacket(false)); } catch (Throwable ignored) {}
    }
    @Override public void sendHurtAnimation(float yaw) {}
    @Override public void sendLinks(org.bukkit.ServerLinks links) {}
    @Override public void addCustomChatCompletions(Collection<String> completions) {}
    @Override public void removeCustomChatCompletions(Collection<String> completions) {}
    @Override public void setCustomChatCompletions(Collection<String> completions) {}
    @Override public void updateInventory() {
        getHandle().inventoryMenu.broadcastChanges();
    }
    @Override public GameMode getPreviousGameMode() { return null; }
    @Override public void setPlayerTime(long time, boolean relative) {
        this.playerTimeRelative = relative;
        this.playerTimeOffset = relative ? time : time - getWorld().getTime();
    }
    @Override public long getPlayerTime() { return getWorld().getTime() + playerTimeOffset; }
    @Override public long getPlayerTimeOffset() { return playerTimeOffset; }
    @Override public boolean isPlayerTimeRelative() { return playerTimeRelative; }
    @Override public void resetPlayerTime() { playerTimeOffset = 0L; playerTimeRelative = true; }
    @Override public void setPlayerWeather(org.bukkit.WeatherType type) { playerWeather = type; }
    @Override public org.bukkit.WeatherType getPlayerWeather() { return playerWeather; }
    @Override public void resetPlayerWeather() { playerWeather = null; }
    @Override public int getExpCooldown() { return 0; }
    @Override public void setExpCooldown(int ticks) {}
    @Override public void giveExp(int amount, boolean applyMending) { getHandle().giveExperiencePoints(amount); }
    @Override public int applyMending(int amount) { return amount; }
    @Override public void giveExpLevels(int levels) { getHandle().giveExperienceLevels(levels); }
    @Override public void setExp(float exp) { getHandle().experienceProgress = exp; getHandle().connection.send(new net.minecraft.network.protocol.game.ClientboundSetExperiencePacket(exp, getHandle().totalExperience, getHandle().experienceLevel)); }
    @Override public float getExp() { return getHandle().experienceProgress; }
    @Override public void sendExperienceChange(float progress, int level) { getHandle().connection.send(new net.minecraft.network.protocol.game.ClientboundSetExperiencePacket(progress, getHandle().totalExperience, level)); }
    @Override public void setExperienceLevelAndProgress(int level) { setLevel(level); }
    @Override public void setLevel(int level) { getHandle().experienceLevel = level; getHandle().connection.send(new net.minecraft.network.protocol.game.ClientboundSetExperiencePacket(getExp(), getHandle().totalExperience, level)); }
    @Override public int getLevel() { return getHandle().experienceLevel; }
    @Override public void setTotalExperience(int exp) { getHandle().totalExperience = exp; }
    @Override public int getTotalExperience() { return getHandle().totalExperience; }
    @Override public int getExperiencePointsNeededForNextLevel() {
        int level = getLevel();
        return level >= 30 ? 9 * level - 158 : level >= 15 ? 5 * level - 38 : 2 * level + 7;
    }
    @Override public int calculateTotalExperiencePoints() {
        int level = getLevel();
        int base;
        if (level <= 16) base = level * level + 6 * level;
        else if (level <= 31) base = (int) Math.floor(2.5 * level * level - 40.5 * level + 360);
        else base = (int) Math.floor(4.5 * level * level - 162.5 * level + 2220);
        return base + Math.round(getExp() * getExperiencePointsNeededForNextLevel());
    }
    @Override public boolean isChunkSent(long chunk) { return false; }
    @Override public Set<Chunk> getSentChunks() { return Collections.emptySet(); }
    @Override public Set<Long> getSentChunkKeys() { return Collections.emptySet(); }
    @Override public void resetIdleDuration() {}
    @Override public Duration getIdleDuration() { return Duration.ZERO; }
    @Override public void setRotation(float yaw, float pitch) {
        getHandle().moveTo(getHandle().getX(), getHandle().getY(), getHandle().getZ(), yaw, pitch);
        getHandle().connection.teleport(getHandle().getX(), getHandle().getY(), getHandle().getZ(), yaw, pitch);
    }
    @Override public void lookAt(double x, double y, double z, io.papermc.paper.entity.LookAnchor anchor) {}
    @Override public void lookAt(Entity entity, io.papermc.paper.entity.LookAnchor anchor, io.papermc.paper.entity.LookAnchor anchor2) {}
    @Override public void showElderGuardian(boolean silent) {}
    @Override public int getWardenWarningCooldown() { return 0; }
    @Override public void setWardenWarningCooldown(int cooldown) {}
    @Override public int getWardenTimeSinceLastWarning() { return 0; }
    @Override public void setWardenTimeSinceLastWarning(int time) {}
    @Override public int getWardenWarningLevel() { return 0; }
    @Override public void setWardenWarningLevel(int level) {}
    @Override public void increaseWardenWarningLevel() {}
    @Override public String getClientBrandName() { return "vanilla"; }
    @Override public void sendOpLevel(byte level) {}
    @Override public void addAdditionalChatCompletions(Collection<String> completions) {}
    @Override public void removeAdditionalChatCompletions(Collection<String> completions) {}
    @Override public float getCooldownPeriod() { return 1.0f; }
    @Override public float getCooledAttackStrength(float adjustTicks) { return getHandle().getAttackStrengthScale(adjustTicks); }
    @Override public void resetCooldown() { getHandle().resetAttackStrengthTicker(); }
    @Override public <T> T getClientOption(com.destroystokyo.paper.ClientOption<T> option) { return null; }
    @Override public PlayerProfile getPlayerProfile() {
        return new io.ampznetwork.lunararc.common.server.LunarArcPlayerProfile(getUniqueId(), getName());
    }
    @Override public void setPlayerProfile(PlayerProfile profile) {}
    @Override public boolean isAllowingServerListings() { return true; }
    @Override public void showDemoScreen() {}
    @Override public void updateCommands() {
        try {
            getHandle().server.getCommands().sendCommands(getHandle());
        } catch (Throwable ignored) {}
    }
    @Override public void openBook(ItemStack book) {}
    @Override public void openSign(Sign sign) {}
    @Override public void openSign(Sign sign, Side side) {}
    @Override public int getClientViewDistance() { return 10; }
    @Override public int getPing() {
        try { return getHandle().connection.latency(); } catch (Throwable t) { return 0; }
    }
    @Override public String getLocale() {
        try { return (String) net.minecraft.server.level.ServerPlayer.class.getMethod("getLanguage").invoke(getHandle()); } catch (Throwable t) { return "en_us"; }
    }
    @Override public java.util.Locale locale() {
        try {
            String tag = getLocale().replace('_', '-');
            return java.util.Locale.forLanguageTag(tag);
        } catch (Throwable t) { return java.util.Locale.US; }
    }
    @Override public boolean getAffectsSpawning() { return true; }
    @Override public void setAffectsSpawning(boolean value) {}
    @Override public int getViewDistance() { return 10; }
    @Override public void setViewDistance(int distance) {}
    @Override public int getSimulationDistance() { return 10; }
    @Override public void setSimulationDistance(int distance) {}
    @Override public int getSendViewDistance() { return 10; }
    @Override public void setSendViewDistance(int distance) {}
    @Override public org.bukkit.advancement.AdvancementProgress getAdvancementProgress(org.bukkit.advancement.Advancement advancement) { return null; }
    @Override public void spawnParticle(Particle particle, Location location, int count) {}
    @Override public void spawnParticle(Particle particle, double x, double y, double z, int count) {}
    @Override public <T> void spawnParticle(Particle particle, Location location, int count, T data) {}
    @Override public <T> void spawnParticle(Particle particle, double x, double y, double z, int count, T data) {}
    @Override public void spawnParticle(Particle particle, Location location, int count, double offsetX, double offsetY, double offsetZ) {}
    @Override public void spawnParticle(Particle particle, double x, double y, double z, int count, double offsetX, double offsetY, double offsetZ) {}
    @Override public <T> void spawnParticle(Particle particle, Location location, int count, double offsetX, double offsetY, double offsetZ, T data) {}
    @Override public <T> void spawnParticle(Particle particle, double x, double y, double z, int count, double offsetX, double offsetY, double offsetZ, T data) {}
    @Override public void spawnParticle(Particle particle, Location location, int count, double offsetX, double offsetY, double offsetZ, double extra) {}
    @Override public void spawnParticle(Particle particle, double x, double y, double z, int count, double offsetX, double offsetY, double offsetZ, double extra) {}
    @Override public <T> void spawnParticle(Particle particle, Location location, int count, double offsetX, double offsetY, double offsetZ, double extra, T data) {}
    @Override public <T> void spawnParticle(Particle particle, double x, double y, double z, int count, double offsetX, double offsetY, double offsetZ, double extra, T data) {}
    @Override public <T> void spawnParticle(Particle particle, Location location, int count, double offsetX, double offsetY, double offsetZ, double extra, T data, boolean force) {}
    @Override public <T> void spawnParticle(Particle particle, double x, double y, double z, int count, double offsetX, double offsetY, double offsetZ, double extra, T data, boolean force) {}
    @Override public org.bukkit.scoreboard.Scoreboard getScoreboard() {
        if (scoreboard == null) scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        return scoreboard;
    }
    @Override public void setScoreboard(org.bukkit.scoreboard.Scoreboard scoreboard) {
        if (scoreboard == null) throw new IllegalArgumentException("Scoreboard cannot be null");
        this.scoreboard = scoreboard;
    }
    @Override public org.bukkit.WorldBorder getWorldBorder() { return null; }
    @Override public void sendEntityEffect(@NotNull EntityEffect effect, @NotNull Entity target) {
        if (!(target instanceof CraftEntity craftTarget)) {
            return;
        }
        if (!effect.getApplicable().isAssignableFrom(target.getClass())) {
            return;
        }
        getHandle().connection.send(new net.minecraft.network.protocol.game.ClientboundEntityEventPacket(
            craftTarget.entity, effect.getData()
        ));
    }
    @Override public void setWorldBorder(org.bukkit.WorldBorder border) {}
    @Override public boolean isHealthScaled() { return false; }
    @Override public void setHealthScaled(boolean scale) {}
    @Override public void setHealthScale(double scale) {}
    @Override public double getHealthScale() { return 20.0; }
    @Override public Entity getSpectatorTarget() { return null; }
    @Override public void setSpectatorTarget(Entity entity) {}
    @SuppressWarnings("deprecation")
    @Override public void sendActionBar(char alternateChar, @NotNull String message) {
        sendActionBar(org.bukkit.ChatColor.translateAlternateColorCodes(alternateChar, message));
    }

    @SuppressWarnings("deprecation")
    @Override public void sendActionBar(@NotNull String message) {
        if (message == null) throw new IllegalArgumentException("message cannot be null");
        io.ampznetwork.lunararc.common.messaging.LunarArcComponentPipeline.sendActionBar(getHandle(), message);
    }

    @SuppressWarnings("deprecation")
    @Override public void sendActionBar(BaseComponent... message) {
        io.ampznetwork.lunararc.common.messaging.LunarArcComponentPipeline.sendActionBar(getHandle(), message);
    }

    @SuppressWarnings("deprecation")
    @Override public void playEffect(@NotNull Location loc, @NotNull Effect effect, int data) {
        playEffect(loc, effect, Integer.valueOf(data));
    }

    @SuppressWarnings({"deprecation", "unchecked"})
    @Override public <T> void playEffect(@NotNull Location loc, @NotNull Effect effect, T data) {
        if (loc == null) throw new IllegalArgumentException("location cannot be null");
        if (effect == null) throw new IllegalArgumentException("effect cannot be null");
        if (loc.getWorld() != getWorld()) return;
        try {
            int value = data instanceof Number number ? number.intValue() : 0;
            getHandle().connection.send(new net.minecraft.network.protocol.game.ClientboundLevelEventPacket(
                effect.getId(),
                new net.minecraft.core.BlockPos(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()),
                value,
                false));
        } catch (Throwable ignored) {}
    }

    @Override public void sendTitle(@NotNull com.destroystokyo.paper.Title title) {
        if (title == null) throw new NullPointerException("title");
        setTitleTimes(title.getFadeIn(), title.getStay(), title.getFadeOut());
        if (title.getSubtitle() != null) setSubtitle(title.getSubtitle());
        if (title.getTitle() != null) showTitle(title.getTitle());
    }
    @Override public void updateTitle(@NotNull com.destroystokyo.paper.Title title) {
        if (title == null) throw new NullPointerException("title");
        // Paper's legacy updateTitle keeps any title parts/timings represented by null/-1
        // unchanged. The vanilla title packets have the same partial-update semantics.
        try {
            if (title.getFadeIn() != -1 || title.getStay() != -1 || title.getFadeOut() != -1) {
                getHandle().connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(
                    title.getFadeIn(), title.getStay(), title.getFadeOut()));
            }
            if (title.getSubtitle() != null) {
                getHandle().connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(
                    io.ampznetwork.lunararc.common.messaging.LunarArcComponentPipeline.fromBungee(title.getSubtitle())));
            }
            if (title.getTitle() != null) {
                getHandle().connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(
                    io.ampznetwork.lunararc.common.messaging.LunarArcComponentPipeline.fromBungee(title.getTitle())));
            }
        } catch (Throwable ignored) {}
    }
    @SuppressWarnings("deprecation")
    @Override public void setTitleTimes(int fadeInTicks, int stayTicks, int fadeOutTicks) {
        try {
            getHandle().connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(fadeInTicks, stayTicks, fadeOutTicks));
        } catch (Throwable ignored) {}
    }

    @SuppressWarnings("deprecation")
    @Override public void setSubtitle(@Nullable BaseComponent[] subtitle) {
        if (subtitle == null) return;
        try {
            getHandle().connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(
                io.ampznetwork.lunararc.common.messaging.LunarArcComponentPipeline.fromBungee(subtitle)));
        } catch (Throwable ignored) {}
    }

    @SuppressWarnings("deprecation")
    @Override public void setSubtitle(@Nullable BaseComponent subtitle) {
        setSubtitle(subtitle == null ? null : new BaseComponent[] { subtitle });
    }

    @SuppressWarnings("deprecation")
    @Override public void showTitle(@Nullable BaseComponent[] title) {
        if (title == null) return;
        try {
            getHandle().connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(
                io.ampznetwork.lunararc.common.messaging.LunarArcComponentPipeline.fromBungee(title)));
        } catch (Throwable ignored) {}
    }

    @SuppressWarnings("deprecation")
    @Override public void showTitle(@Nullable BaseComponent title) {
        showTitle(title == null ? null : new BaseComponent[] { title });
    }

    @SuppressWarnings("deprecation")
    @Override public void showTitle(@Nullable BaseComponent[] title, @Nullable BaseComponent[] subtitle, int fadeInTicks, int stayTicks, int fadeOutTicks) {
        setTitleTimes(fadeInTicks, stayTicks, fadeOutTicks);
        if (subtitle != null) setSubtitle(subtitle);
        if (title != null) showTitle(title);
    }

    @SuppressWarnings("deprecation")
    @Override public void showTitle(@Nullable BaseComponent title, @Nullable BaseComponent subtitle, int fadeInTicks, int stayTicks, int fadeOutTicks) {
        showTitle(
            title == null ? null : new BaseComponent[] { title },
            subtitle == null ? null : new BaseComponent[] { subtitle },
            fadeInTicks, stayTicks, fadeOutTicks
        );
    }

    @Override public void sendTitle(String title, String subtitle) { sendTitle(title, subtitle, 10, 70, 20); }
    @Override public void sendTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        try {
            getHandle().connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut));
            if (subtitle != null) getHandle().connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(
                    io.ampznetwork.lunararc.common.messaging.LunarArcComponentPipeline.fromLegacy(subtitle)));
            if (title != null) getHandle().connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(
                    io.ampznetwork.lunararc.common.messaging.LunarArcComponentPipeline.fromLegacy(title)));
        } catch (Throwable ignored) {}
    }
    @Override public void resetTitle() {
        try { getHandle().connection.send(new net.minecraft.network.protocol.game.ClientboundClearTitlesPacket(true)); } catch (Throwable ignored) {}
    }
    @Override public void removeResourcePacks() {}
    @Override public void removeResourcePack(UUID id) {}
    @Override public void clearResourcePacks() {}
    @Override public void sendResourcePacks(@NotNull net.kyori.adventure.resource.ResourcePackRequest request) {}
    @Override public void addResourcePack(UUID id, String url, byte[] hash, String prompt, boolean force) {}
    @Override public org.bukkit.event.player.PlayerResourcePackStatusEvent.Status getResourcePackStatus() { return org.bukkit.event.player.PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED; }
    @Override public void setResourcePack(String url) {}
    @Override public void setResourcePack(String url, byte[] hash) {}
    @Override public void setResourcePack(String url, byte[] hash, String prompt) {}
    @Override public void setResourcePack(String url, byte[] hash, boolean force) {}
    @Override public void setResourcePack(String url, byte[] hash, String prompt, boolean force) {}
    @Override public void setResourcePack(UUID id, String url, byte[] hash, String prompt, boolean force) {}
    @Override public void setResourcePack(UUID id, String url, byte[] hash, net.kyori.adventure.text.Component prompt, boolean force) {}
    @Override public void setTexturePack(String url) {}
    @Override public void setResourcePack(String url, byte[] hash, net.kyori.adventure.text.Component prompt, boolean force) {}
    @Override public String getResourcePackHash() { return null; }
    

    // Remaining Bukkit/Paper Player surface audited against Paper 1.21.1.
    @Override
    public boolean breakBlock(@NotNull Block block) {
        if (block == null) throw new IllegalArgumentException("block cannot be null");
        if (block.getWorld() != getWorld()) return false;
        return block.breakNaturally(getInventory().getItemInMainHand());
    }

    @SuppressWarnings("deprecation")
    @Override
    public @Nullable Location getBedSpawnLocation() {
        return getRespawnLocation();
    }

    @Override
    public @Nullable Location getRespawnLocation() {
        return respawnLocation == null ? null : respawnLocation.clone();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void setBedSpawnLocation(@Nullable Location location) {
        setRespawnLocation(location);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void setBedSpawnLocation(@Nullable Location location, boolean force) {
        setRespawnLocation(location, force);
    }

    @Override
    public void setRespawnLocation(@Nullable Location location) {
        setRespawnLocation(location, false);
    }

    @Override
    public void setRespawnLocation(@Nullable Location location, boolean force) {
        this.respawnLocation = location == null ? null : location.clone();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void playNote(@NotNull Location loc, byte instrument, byte note) {}

    @Override
    public void playNote(@NotNull Location loc, @NotNull Instrument instrument, @NotNull Note note) {}

    @Override public void playSound(@NotNull Location location, @NotNull String sound, float volume, float pitch) {}
    @Override public void playSound(@NotNull Location location, @NotNull String sound, @NotNull SoundCategory category, float volume, float pitch) {}
    @Override public void playSound(@NotNull Location location, @NotNull String sound, @NotNull SoundCategory category, float volume, float pitch, long seed) {}
    @Override public void playSound(@NotNull Location location, @NotNull Sound sound, float volume, float pitch) {}
    @Override public void playSound(@NotNull Location location, @NotNull Sound sound, @NotNull SoundCategory category, float volume, float pitch) {}
    @Override public void playSound(@NotNull Location location, @NotNull Sound sound, @NotNull SoundCategory category, float volume, float pitch, long seed) {}
    @Override public void playSound(@NotNull Entity entity, @NotNull String sound, float volume, float pitch) {}
    @Override public void playSound(@NotNull Entity entity, @NotNull String sound, @NotNull SoundCategory category, float volume, float pitch) {}
    @Override public void playSound(@NotNull Entity entity, @NotNull String sound, @NotNull SoundCategory category, float volume, float pitch, long seed) {}
    @Override public void playSound(@NotNull Entity entity, @NotNull Sound sound, float volume, float pitch) {}
    @Override public void playSound(@NotNull Entity entity, @NotNull Sound sound, @NotNull SoundCategory category, float volume, float pitch) {}
    @Override public void playSound(@NotNull Entity entity, @NotNull Sound sound, @NotNull SoundCategory category, float volume, float pitch, long seed) {}

    @Override public void stopSound(@NotNull Sound sound) {}
    @Override public void stopSound(@NotNull String sound) {}
    @Override public void stopSound(@NotNull Sound sound, @Nullable SoundCategory category) {}
    @Override public void stopSound(@NotNull String sound, @Nullable SoundCategory category) {}
    @Override public void stopSound(@NotNull SoundCategory category) {}
    @Override public void stopAllSounds() {}

    @Override
    public @Nullable BanEntry<InetAddress> banIp(@Nullable String reason, @Nullable Duration duration, @Nullable String source, boolean kickPlayer) {
        InetAddress address = getAddress() == null ? null : getAddress().getAddress();
        if (address == null) return null;
        BanEntry<InetAddress> entry = org.bukkit.craftbukkit.v1_21_R1.CraftBanList.IP_BANS.addBan(address, reason, duration, source);
        if (kickPlayer) kick(Component.text(reason == null ? "Banned by an operator." : reason));
        return entry;
    }

    @Override
    public @Nullable BanEntry<InetAddress> banIp(@Nullable String reason, @Nullable Instant expires, @Nullable String source, boolean kickPlayer) {
        InetAddress address = getAddress() == null ? null : getAddress().getAddress();
        if (address == null) return null;
        BanEntry<InetAddress> entry = org.bukkit.craftbukkit.v1_21_R1.CraftBanList.IP_BANS.addBan(address, reason, expires, source);
        if (kickPlayer) kick(Component.text(reason == null ? "Banned by an operator." : reason));
        return entry;
    }

    @Override
    public @Nullable BanEntry<InetAddress> banIp(@Nullable String reason, @Nullable Date expires, @Nullable String source, boolean kickPlayer) {
        InetAddress address = getAddress() == null ? null : getAddress().getAddress();
        if (address == null) return null;
        BanEntry<InetAddress> entry = org.bukkit.craftbukkit.v1_21_R1.CraftBanList.IP_BANS.addBan(address, reason, expires, source);
        if (kickPlayer) kick(Component.text(reason == null ? "Banned by an operator." : reason));
        return entry;
    }

    // LivingEntity
    @Override public Player getKiller() { return null; }
    @Override public void setArrowsInBody(int count, boolean remove) {}
    @Override public org.bukkit.Sound getFallDamageSoundBig() { return org.bukkit.Sound.ENTITY_PLAYER_BIG_FALL; }
    @Override public org.bukkit.Sound getFallDamageSoundSmall() { return org.bukkit.Sound.ENTITY_PLAYER_SMALL_FALL; }
    @Override public Entity getTargetEntity(int distance, boolean nonTransparent) { return null; }
    @Override public RayTraceResult rayTraceEntities(int distance, boolean nonTransparent) { return null; }
    @Override public void broadcastSlotBreak(EquipmentSlot slot) {}
    @Override public void broadcastSlotBreak(EquipmentSlot slot, Collection<Player> players) {}
    @Override public void setGliding(boolean gliding) {
        if (!gliding && getHandle().isFallFlying()) getHandle().stopFallFlying();
        else if (gliding && !getHandle().isFallFlying()) getHandle().startFallFlying();
    }
    @Override public void attack(Entity target) {
        if (target instanceof CraftEntity craftEntity) getHandle().attack(craftEntity.entity);
    }
    @Override public boolean canBreatheUnderwater() { return false; }
    @Override public void setKiller(Player killer) {}
    @Override public @Nullable Block getTargetBlock(int distance, com.destroystokyo.paper.block.TargetBlockInfo.FluidMode fluidMode) { 
        RayTraceResult result = rayTraceBlocks(distance, fluidMode == com.destroystokyo.paper.block.TargetBlockInfo.FluidMode.ALWAYS ? FluidCollisionMode.ALWAYS : FluidCollisionMode.NEVER);
        return result != null ? result.getHitBlock() : null;
    }
    @Override public @NotNull Block getTargetBlock(Set<Material> transparent, int distance) { return (Block) java.lang.reflect.Proxy.newProxyInstance(Block.class.getClassLoader(), new Class<?>[] { Block.class }, (p, m, a) -> null); }
    @Override public @NotNull List<Block> getLastTwoTargetBlocks(Set<Material> transparent, int distance) { return Collections.emptyList(); }
    @Override public int getNoDamageTicks() { return getHandle().invulnerableTime; }
    @Override public EntityCategory getCategory() { return EntityCategory.NONE; }
    @Override public void setRemainingAir(int air) { getHandle().setAirSupply(air); }
    @Override public double getEyeHeight() { return getHandle().getEyeHeight(); }
    @Override public double getEyeHeight(boolean ignorePose) { return getHandle().getEyeHeight(); }
    @Override public float getBodyYaw() { return getHandle().yBodyRot; }
    @Override public void setBodyYaw(float yaw) { getHandle().yBodyRot = yaw; }
    @Override public org.bukkit.Sound getDrinkingSound(ItemStack itemStack) { return org.bukkit.Sound.ENTITY_GENERIC_DRINK; }
    @Override public boolean hasLineOfSight(Location location) {
        if (location == null || location.getWorld() != getWorld()) return false;
        org.bukkit.util.Vector direction = location.toVector().subtract(getEyeLocation().toVector());
        double distance = direction.length();
        org.bukkit.util.RayTraceResult hit = rayTraceBlocks(distance, FluidCollisionMode.NEVER);
        return hit == null || hit.getHitPosition().distanceSquared(getEyeLocation().toVector()) + 1.0E-6 >= distance * distance;
    }
    @Override public boolean hasLineOfSight(Entity other) {
        return other instanceof CraftEntity craftEntity && getHandle().hasLineOfSight(craftEntity.entity);
    }
    @Override public org.bukkit.Sound getHurtSound() { return org.bukkit.Sound.ENTITY_PLAYER_HURT; }
    @Override public org.bukkit.Sound getDeathSound() { return org.bukkit.Sound.ENTITY_PLAYER_DEATH; }
    @Override public Block getTargetBlockExact(int distance) {
        return getTargetBlockExact(distance, FluidCollisionMode.NEVER);
    }
    @Override public Block getTargetBlockExact(int distance, FluidCollisionMode fluidCollisionMode) {
        RayTraceResult result = rayTraceBlocks(distance, fluidCollisionMode);
        return result == null ? null : result.getHitBlock();
    }
    @Override public float getUpwardsMovement() { return 0; }
    @Override public float getSidewaysMovement() { return 0; }
    @Override public float getForwardsMovement() { return 0; }
    @Override public boolean isRiptiding() { return getHandle().isAutoSpinAttack(); }
    @Override public boolean isClimbing() { return getHandle().onClimbable(); }
    @Override
    public Collection<PotionEffect> getActivePotionEffects() {
        java.util.List<PotionEffect> out = new java.util.ArrayList<>();
        for (net.minecraft.world.effect.MobEffectInstance fx : getHandle().getActiveEffects()) {
            PotionEffectType type = PotionEffectType.getByKey(
                org.bukkit.NamespacedKey.fromString(
                    net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getKey(fx.getEffect().value()).toString()));
            if (type != null)
                out.add(new PotionEffect(type, fx.getDuration(), fx.getAmplifier(), fx.isAmbient(), fx.isVisible()));
        }
        return out;
    }
    @Override
    public boolean hasPotionEffect(PotionEffectType type) {
        if (type == null) return false;
        net.minecraft.resources.ResourceLocation rl = net.minecraft.resources.ResourceLocation.parse(type.getKey().toString());
        var effectOpt = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getHolder(rl);
        return effectOpt.isPresent() && getHandle().hasEffect(effectOpt.get());
    }
    @Override public void knockback(double strength, double x, double z) {}
    @Override public void setShieldBlockingDelay(int delay) {}
    @Override public void setArrowCooldown(int ticks) {}
    @Override public void setNoActionTicks(int ticks) {}
    @Override public void completeUsingActiveItem() {}
    @Override public boolean canUseEquipmentSlot(EquipmentSlot slot) { return true; }
    @Override public <T> void setMemory(MemoryKey<T> key, T value) {}
    @Override public <T> T getMemory(MemoryKey<T> key) { return null; }
    @Override public float getHurtDirection() { return 0; }
    @Override public void setHurtDirection(float direction) {}
    @Override public void damageItemStack(EquipmentSlot slot, int amount) {}
    @Override public ItemStack damageItemStack(ItemStack stack, int amount) { return stack; }
    @Override public void setNoDamageTicks(int ticks) { getHandle().invulnerableTime = Math.max(0, ticks); }
    @Override public boolean isGliding() { return getHandle().isFallFlying(); }
    @Override public Set<UUID> getCollidableExemptions() { return Collections.emptySet(); }
    @Override public org.bukkit.Sound getFallDamageSound(int fallDistance) { return org.bukkit.Sound.ENTITY_GENERIC_SMALL_FALL; }
    @Override public void setNextArrowRemoval(int ticks) {}
    @Override public Entity getLeashHolder() { return null; }
    @Override public org.bukkit.block.BlockFace getTargetBlockFace(int distance, com.destroystokyo.paper.block.TargetBlockInfo.FluidMode fluidMode) { return null; }
    @Override public org.bukkit.block.BlockFace getTargetBlockFace(int distance, FluidCollisionMode fluidMode) { return null; }

    // HumanEntity
    @Override public boolean setWindowProperty(InventoryView.Property prop, int value) {
        if (prop == null) return false;
        return getOpenInventory().setProperty(prop, value);
    }
    @Override public void wakeup(boolean setSpawnLocation) {
        if (!getHandle().isSleeping()) throw new IllegalStateException("Cannot wakeup if not sleeping");
        getHandle().stopSleepInBed(true, setSpawnLocation);
    }
    @Override public ItemStack getItemOnCursor() {
        return CraftItemStack.asBukkitCopy(getHandle().containerMenu.getCarried());
    }
    @Override public void setItemOnCursor(ItemStack item) {
        getHandle().containerMenu.setCarried(CraftItemStack.asNMSCopy(item));
        getHandle().containerMenu.broadcastCarriedItem();
    }
    private final Set<NamespacedKey> lunararcDiscoveredRecipes = ConcurrentHashMap.newKeySet();
    private java.util.Collection<net.minecraft.world.item.crafting.RecipeHolder<?>> resolveRecipes(Collection<NamespacedKey> recipes) {
        java.util.List<net.minecraft.world.item.crafting.RecipeHolder<?>> resolved = new java.util.ArrayList<>();
        var manager = getHandle().server.getRecipeManager();
        for (NamespacedKey key : recipes) {
            if (key == null) continue;
            try {
                var id = net.minecraft.resources.ResourceLocation.parse(key.toString());
                manager.byKey(id).ifPresent(resolved::add);
            } catch (Throwable ignored) {}
        }
        return resolved;
    }
    @Override public int discoverRecipes(Collection<NamespacedKey> recipes) {
        if (recipes == null) throw new IllegalArgumentException("recipes cannot be null");
        var resolved = resolveRecipes(recipes);
        int changed = getHandle().awardRecipes(resolved);
        for (NamespacedKey key : recipes) if (key != null) lunararcDiscoveredRecipes.add(key);
        return changed;
    }
    @Override public int undiscoverRecipes(Collection<NamespacedKey> recipes) {
        if (recipes == null) throw new IllegalArgumentException("recipes cannot be null");
        var resolved = resolveRecipes(recipes);
        int changed = getHandle().resetRecipes(resolved);
        lunararcDiscoveredRecipes.removeAll(recipes);
        return changed;
    }
    @Override public EntityEquipment getEquipment() { return inventory; }
    @Override public void setStarvationRate(int rate) {
        if (rate < 0) throw new IllegalArgumentException("rate must be >= 0");
        ((io.ampznetwork.lunararc.common.mixin.core.player.FoodDataAccessor) getHandle().getFoodData()).setStarvationRate(rate);
    }
    @Override public boolean isBlocking() { return getHandle().isBlocking(); }
    @Override public void setSaturatedRegenRate(int rate) {
        if (rate < 0) throw new IllegalArgumentException("rate must be >= 0");
        ((io.ampznetwork.lunararc.common.mixin.core.player.FoodDataAccessor) getHandle().getFoodData()).setSaturatedRegenRate(rate);
    }
    @Override public float getExhaustion() {
        return ((io.ampznetwork.lunararc.common.mixin.core.player.FoodDataAccessor) getHandle().getFoodData()).getExhaustionLevel();
    }
    private net.minecraft.world.item.Item resolveItem(Material material) {
        if (material == null) throw new IllegalArgumentException("material cannot be null");
        if (!material.isItem()) throw new IllegalArgumentException(material + " is not an item");
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(material.getKey().toString()));
    }
    @Override public boolean hasCooldown(Material material) { return getHandle().getCooldowns().isOnCooldown(resolveItem(material)); }
    @Override public void setCooldown(Material material, int ticks) {
        if (ticks < 0) throw new IllegalArgumentException("ticks must be >= 0");
        getHandle().getCooldowns().addCooldown(resolveItem(material), ticks);
    }
    @Override public GameMode getGameMode() { return fromNMS(getHandle().gameMode.getGameModeForPlayer()); }
    @Override public void setGameMode(GameMode mode) {
        if (mode == null) throw new IllegalArgumentException("GameMode cannot be null");
        getHandle().setGameMode(toNMS(mode));
        syncAbilities();
    }
    @Override public Entity releaseRightShoulderEntity() {
        try { var nms = getHandle().releaseRightShoulderEntity(); return nms == null ? null : ((EntityBridge)nms).lunararc$getBukkitEntity(); }
        catch (Throwable ignored) { return null; }
    }
    @Override public Entity releaseLeftShoulderEntity() {
        try { var nms = getHandle().releaseLeftShoulderEntity(); return nms == null ? null : ((EntityBridge)nms).lunararc$getBukkitEntity(); }
        catch (Throwable ignored) { return null; }
    }
    private Entity shoulderEntity(net.minecraft.nbt.CompoundTag tag) {
        if (tag == null || tag.isEmpty()) return null;
        try {
            var created = net.minecraft.world.entity.EntityType.create(tag, getHandle().level());
            if (created.isEmpty()) return null;
            return ((EntityBridge) created.get()).lunararc$getBukkitEntity();
        } catch (Throwable ignored) { return null; }
    }
    @Override public Entity getShoulderEntityLeft() { return shoulderEntity(getHandle().getShoulderEntityLeft()); }
    @Override public Entity getShoulderEntityRight() { return shoulderEntity(getHandle().getShoulderEntityRight()); }
    private net.minecraft.nbt.CompoundTag saveShoulder(Entity entity) {
        if (entity == null) return new net.minecraft.nbt.CompoundTag();
        if (!(entity instanceof CraftEntity craft)) throw new IllegalArgumentException("Entity is not a LunarArc/Craft entity");
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        craft.getHandle().save(tag);
        entity.remove();
        return tag;
    }
    @Override public void setShoulderEntityLeft(Entity entity) { getHandle().setShoulderEntityLeft(saveShoulder(entity)); }
    @Override public void setShoulderEntityRight(Entity entity) { getHandle().setShoulderEntityRight(saveShoulder(entity)); }
    @Override public boolean hasDiscoveredRecipe(NamespacedKey recipe) { return recipe != null && lunararcDiscoveredRecipes.contains(recipe); }
    @Override public int getUnsaturatedRegenRate() { return ((io.ampznetwork.lunararc.common.mixin.core.player.FoodDataAccessor) getHandle().getFoodData()).getUnsaturatedRegenRate(); }
    @Override public int getSleepTicks() { return getHandle().getSleepTimer(); }
    @Override public boolean undiscoverRecipe(NamespacedKey recipe) { return undiscoverRecipes(java.util.Collections.singleton(recipe)) != 0; }
    @Override public boolean sleep(Location location, boolean force) {
        if (location == null || location.getWorld() == null) throw new IllegalArgumentException("location/world cannot be null");
        if (!location.getWorld().equals(getWorld())) throw new IllegalArgumentException("Cannot sleep across worlds");
        var pos = new net.minecraft.core.BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        var state = getHandle().level().getBlockState(pos);
        if (!(state.getBlock() instanceof net.minecraft.world.level.block.BedBlock)) return false;
        try { return getHandle().startSleepInBed(pos).left().isEmpty(); }
        catch (Throwable t) { return false; }
    }
    @Override public boolean isDeeplySleeping() { return getHandle().isSleepingLongEnough(); }
    @Override public int getStarvationRate() { return ((io.ampznetwork.lunararc.common.mixin.core.player.FoodDataAccessor) getHandle().getFoodData()).getStarvationRate(); }
    @Override public Set<NamespacedKey> getDiscoveredRecipes() { return java.util.Collections.unmodifiableSet(new java.util.LinkedHashSet<>(lunararcDiscoveredRecipes)); }
    @Override public int getSaturatedRegenRate() { return ((io.ampznetwork.lunararc.common.mixin.core.player.FoodDataAccessor) getHandle().getFoodData()).getSaturatedRegenRate(); }
    @Override public Location getBedLocation() {
        var pos = getHandle().getSleepingPos().orElseThrow(() -> new IllegalStateException("Not sleeping"));
        return new Location(getWorld(), pos.getX(), pos.getY(), pos.getZ());
    }
    @Override public void setSaturation(float value) {
        ((io.ampznetwork.lunararc.common.mixin.core.player.FoodDataAccessor) getHandle().getFoodData()).setSaturationLevel(value);
    }
    @Override public FishHook getFishHook() {
        try { return getHandle().fishing == null ? null : (FishHook) ((EntityBridge)getHandle().fishing).lunararc$getBukkitEntity(); }
        catch (Throwable ignored) { return null; }
    }
    @Override public void startRiptideAttack(int duration, float attackDamage, ItemStack itemStack) {
        if (duration <= 0) throw new IllegalArgumentException("duration must be > 0");
        if (attackDamage < 0) throw new IllegalArgumentException("attackDamage must be >= 0");
        getHandle().startAutoSpinAttack(duration, attackDamage, CraftItemStack.asNMSCopy(itemStack));
    }
    @Override public boolean isHandRaised() { return getHandle().isUsingItem(); }
    @Override public void setLastDeathLocation(Location loc) { this.lastDeathLocation = loc == null ? null : loc.clone(); }
    private Location lastDeathLocation;
    @Override public int getCooldown(Material material) {
        Object cooldowns = getHandle().getCooldowns();
        Object item = resolveItem(material);
        try {
            java.lang.reflect.Field mapField = cooldowns.getClass().getDeclaredField("cooldowns"); mapField.setAccessible(true);
            Object map = mapField.get(cooldowns);
            Object entry = ((java.util.Map<?,?>) map).get(item);
            if (entry == null) return 0;
            java.lang.reflect.Field end = entry.getClass().getDeclaredField("endTime"); end.setAccessible(true);
            java.lang.reflect.Field tick = cooldowns.getClass().getDeclaredField("tickCount"); tick.setAccessible(true);
            return Math.max(0, end.getInt(entry) - tick.getInt(cooldowns));
        } catch (Throwable ignored) { return hasCooldown(material) ? 1 : 0; }
    }
    @Override public void setUnsaturatedRegenRate(int rate) {
        if (rate < 0) throw new IllegalArgumentException("rate must be >= 0");
        ((io.ampznetwork.lunararc.common.mixin.core.player.FoodDataAccessor) getHandle().getFoodData()).setUnsaturatedRegenRate(rate);
    }
    @Override public boolean dropItem(boolean dropAll) {
        boolean success = getHandle().drop(dropAll);
        if (success) getHandle().containerMenu.broadcastChanges();
        return success;
    }
    @Override public ItemStack getItemInHand() { return CraftItemStack.asBukkitCopy(getHandle().getMainHandItem()); }
    @Override public void setItemInHand(ItemStack item) {
        getHandle().getInventory().setItem(getHandle().getInventory().selected, CraftItemStack.asNMSCopy(item));
        getHandle().inventoryMenu.broadcastChanges();
    }
    @Override public float getSaturation() { return getHandle().getFoodData().getSaturationLevel(); }
    @Override public void setExhaustion(float value) { ((io.ampznetwork.lunararc.common.mixin.core.player.FoodDataAccessor) getHandle().getFoodData()).setExhaustionLevel(value); }
    @Override public float getAttackCooldown() { return getHandle().getAttackStrengthScale(0.5F); }
    @Override public int getExpToLevel() { return getHandle().getXpNeededForNextLevel(); }
    @Override public Location getPotentialBedLocation() {
        var pos = getHandle().getRespawnPosition();
        if (pos == null) return null;
        var level = getHandle().server.getLevel(getHandle().getRespawnDimension());
        if (level == null) return null;
        org.bukkit.World bw = server.getWorld(level.dimension().location().toString());
        if (bw == null) bw = getWorld();
        return new Location(bw, pos.getX(), pos.getY(), pos.getZ());
    }
    @Override public PlayerInventory getInventory() { return inventory; }
    @Override public Inventory getEnderChest() { return enderChest; }
    @Override public MainHand getMainHand() { return getHandle().getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT ? MainHand.LEFT : MainHand.RIGHT; }

    private InventoryView makeView(net.minecraft.world.inventory.AbstractContainerMenu menu, Inventory top, org.bukkit.event.inventory.InventoryType type, net.kyori.adventure.text.Component title) {
        return new org.bukkit.craftbukkit.v1_21_R1.inventory.CraftInventoryView(this, menu, top, inventory, type, title);
    }
    private org.bukkit.event.inventory.InventoryType inferInventoryType(net.minecraft.world.inventory.AbstractContainerMenu menu) {
        if (menu == getHandle().inventoryMenu) return org.bukkit.event.inventory.InventoryType.CRAFTING;
        if (menu instanceof net.minecraft.world.inventory.ChestMenu) return org.bukkit.event.inventory.InventoryType.CHEST;
        if (menu instanceof net.minecraft.world.inventory.CraftingMenu) return org.bukkit.event.inventory.InventoryType.WORKBENCH;
        if (menu instanceof net.minecraft.world.inventory.FurnaceMenu) return org.bukkit.event.inventory.InventoryType.FURNACE;
        if (menu instanceof net.minecraft.world.inventory.BlastFurnaceMenu) return org.bukkit.event.inventory.InventoryType.BLAST_FURNACE;
        if (menu instanceof net.minecraft.world.inventory.SmokerMenu) return org.bukkit.event.inventory.InventoryType.SMOKER;
        if (menu instanceof net.minecraft.world.inventory.AnvilMenu) return org.bukkit.event.inventory.InventoryType.ANVIL;
        if (menu instanceof net.minecraft.world.inventory.EnchantmentMenu) return org.bukkit.event.inventory.InventoryType.ENCHANTING;
        if (menu instanceof net.minecraft.world.inventory.BrewingStandMenu) return org.bukkit.event.inventory.InventoryType.BREWING;
        if (menu instanceof net.minecraft.world.inventory.BeaconMenu) return org.bukkit.event.inventory.InventoryType.BEACON;
        if (menu instanceof net.minecraft.world.inventory.HopperMenu) return org.bukkit.event.inventory.InventoryType.HOPPER;
        if (menu instanceof net.minecraft.world.inventory.ShulkerBoxMenu) return org.bukkit.event.inventory.InventoryType.SHULKER_BOX;
        if (menu instanceof net.minecraft.world.inventory.HorseInventoryMenu) return org.bukkit.event.inventory.InventoryType.CHEST;
        if (menu instanceof net.minecraft.world.inventory.MerchantMenu) return org.bukkit.event.inventory.InventoryType.MERCHANT;
        if (menu instanceof net.minecraft.world.inventory.LecternMenu) return org.bukkit.event.inventory.InventoryType.LECTERN;
        if (menu instanceof net.minecraft.world.inventory.LoomMenu) return org.bukkit.event.inventory.InventoryType.LOOM;
        if (menu instanceof net.minecraft.world.inventory.GrindstoneMenu) return org.bukkit.event.inventory.InventoryType.GRINDSTONE;
        if (menu instanceof net.minecraft.world.inventory.CartographyTableMenu) return org.bukkit.event.inventory.InventoryType.CARTOGRAPHY;
        if (menu instanceof net.minecraft.world.inventory.StonecutterMenu) return org.bukkit.event.inventory.InventoryType.STONECUTTER;
        if (menu instanceof net.minecraft.world.inventory.SmithingMenu) return org.bukkit.event.inventory.InventoryType.SMITHING;
        return org.bukkit.event.inventory.InventoryType.CHEST;
    }
    @Override public InventoryView getOpenInventory() {
        var menu = getHandle().containerMenu;
        org.bukkit.event.inventory.InventoryType type = inferInventoryType(menu);
        return makeView(menu, null, type, net.kyori.adventure.text.Component.text(type.name()));
    }
    @Override public InventoryView openInventory(Inventory opened) {
        if (opened == null) throw new IllegalArgumentException("inventory cannot be null");
        if (getHandle().connection == null) return null;
        int rows = Math.max(1, Math.min(6, (opened.getSize() + 8) / 9));
        if (opened.getSize() != rows * 9) throw new IllegalArgumentException("Unsupported custom inventory size " + opened.getSize() + "; chest inventories must be a multiple of 9 up to 54");
        // Fire InventoryCloseEvent(OPEN_NEW) and close any already-open menu before
        // opening the new one, matching Paper's open flow.
        if (getHandle().containerMenu != getHandle().inventoryMenu) {
            org.bukkit.craftbukkit.v1_21_R1.event.CraftEventFactory.handleInventoryCloseEvent(getHandle(),
                    org.bukkit.event.inventory.InventoryCloseEvent.Reason.OPEN_NEW);
            getHandle().closeContainer();
        }
        var adapter = new org.bukkit.craftbukkit.v1_21_R1.inventory.BukkitInventoryContainer(opened);
        net.kyori.adventure.text.Component adventureTitle = opened instanceof org.bukkit.craftbukkit.v1_21_R1.inventory.CraftInventory ci ? ci.title() : net.kyori.adventure.text.Component.text(opened.getType().name());
        net.minecraft.world.inventory.MenuType<?> menuType = switch (rows) {
            case 1 -> net.minecraft.world.inventory.MenuType.GENERIC_9x1;
            case 2 -> net.minecraft.world.inventory.MenuType.GENERIC_9x2;
            case 3 -> net.minecraft.world.inventory.MenuType.GENERIC_9x3;
            case 4 -> net.minecraft.world.inventory.MenuType.GENERIC_9x4;
            case 5 -> net.minecraft.world.inventory.MenuType.GENERIC_9x5;
            case 6 -> net.minecraft.world.inventory.MenuType.GENERIC_9x6;
            default -> throw new IllegalArgumentException("Unsupported chest rows: " + rows);
        };
        int id = getHandle().nextContainerCounter();
        net.minecraft.world.inventory.AbstractContainerMenu menu = new net.minecraft.world.inventory.ChestMenu(
                menuType, id, getHandle().getInventory(), adapter, rows);
        InventoryView view = makeView(menu, opened, opened.getType(), adventureTitle);
        com.mojang.datafixers.util.Pair<net.kyori.adventure.text.Component, net.minecraft.world.inventory.AbstractContainerMenu> result =
                org.bukkit.craftbukkit.v1_21_R1.event.CraftEventFactory.callInventoryOpenEventWithTitle(
                        getHandle(), menu, view, false);
        if (result.getSecond() == null) return null; // cancelled — the menu was never shown to the client
        getHandle().containerMenu = menu;
        menu.checkReachable = false;
        net.kyori.adventure.text.Component finalTitle = result.getFirst() != null ? result.getFirst() : adventureTitle;
        getHandle().connection.send(new net.minecraft.network.protocol.game.ClientboundOpenScreenPacket(id, menuType,
                io.ampznetwork.lunararc.common.messaging.LunarArcComponentPipeline.fromAdventure(finalTitle)));
        getHandle().initMenu(menu);
        menu.broadcastFullState();
        if (opened instanceof org.bukkit.craftbukkit.v1_21_R1.inventory.CraftInventory ci) ci.onOpen(this);
        return view;
    }
    @Override public void openInventory(InventoryView view) {
        if (view == null) throw new IllegalArgumentException("inventory view cannot be null");
        if (!this.equals(view.getPlayer())) throw new IllegalArgumentException("InventoryView must belong to this player");
        if (view instanceof org.bukkit.craftbukkit.v1_21_R1.inventory.CraftInventoryView civ) {
            if (civ.getHandle() == getHandle().containerMenu) return;
            if (getHandle().containerMenu != getHandle().inventoryMenu) {
                org.bukkit.craftbukkit.v1_21_R1.event.CraftEventFactory.handleInventoryCloseEvent(getHandle(),
                        org.bukkit.event.inventory.InventoryCloseEvent.Reason.OPEN_NEW);
                getHandle().closeContainer();
            }
            com.mojang.datafixers.util.Pair<net.kyori.adventure.text.Component, net.minecraft.world.inventory.AbstractContainerMenu> result =
                    org.bukkit.craftbukkit.v1_21_R1.event.CraftEventFactory.callInventoryOpenEventWithTitle(
                            getHandle(), civ.getHandle(), view, false);
            if (result.getSecond() == null) return; // cancelled
            getHandle().containerMenu = civ.getHandle();
            getHandle().initMenu(civ.getHandle());
        } else {
            openInventory(view.getTopInventory());
        }
    }
    private void notifyClientClose(net.minecraft.world.inventory.AbstractContainerMenu menu) {
        if (getHandle().connection == null || menu == null) return;
        try {
            getHandle().connection.send(new net.minecraft.network.protocol.game.ClientboundContainerClosePacket(menu.containerId));
        } catch (Throwable ignored) {}
    }
    private void applyTitleOverride(net.minecraft.world.inventory.AbstractContainerMenu menu, net.kyori.adventure.text.Component override) {
        if (override == null || getHandle().connection == null || menu == null) return;
        try {
            getHandle().connection.send(new net.minecraft.network.protocol.game.ClientboundOpenScreenPacket(
                    menu.containerId, menu.getType(),
                    io.ampznetwork.lunararc.common.messaging.LunarArcComponentPipeline.fromAdventure(override)));
        } catch (Throwable ignored) {}
    }
    private InventoryView openBlockMenu(Location location, boolean force, Material expected, net.minecraft.world.level.block.Block menuBlock) {
        if (location == null) location = getLocation();
        if (location.getWorld() == null || !location.getWorld().equals(getWorld())) throw new IllegalArgumentException("Location must be in the player's world");
        if (!force && location.getBlock().getType() != expected) return null;
        var pos = new net.minecraft.core.BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        var provider = menuBlock.defaultBlockState().getMenuProvider(getHandle().level(), pos);
        if (provider == null) return null;
        if (getHandle().containerMenu != getHandle().inventoryMenu) {
            org.bukkit.craftbukkit.v1_21_R1.event.CraftEventFactory.handleInventoryCloseEvent(getHandle(),
                    org.bukkit.event.inventory.InventoryCloseEvent.Reason.OPEN_NEW);
            getHandle().closeContainer();
        }
        var before = getHandle().containerMenu;
        getHandle().openMenu(provider);
        if (getHandle().containerMenu == before) return null;
        getHandle().containerMenu.checkReachable = !force;
        InventoryView view = getOpenInventory();
        net.minecraft.world.inventory.AbstractContainerMenu menu = getHandle().containerMenu;
        com.mojang.datafixers.util.Pair<net.kyori.adventure.text.Component, net.minecraft.world.inventory.AbstractContainerMenu> result =
                org.bukkit.craftbukkit.v1_21_R1.event.CraftEventFactory.callInventoryOpenEventWithTitle(
                        getHandle(), menu, view, false);
        if (result.getSecond() == null) {
            getHandle().closeContainer();
            notifyClientClose(menu);
            return null;
        }
        applyTitleOverride(menu, result.getFirst());
        return view;
    }
    @Override public InventoryView openWorkbench(Location location, boolean force) { return openBlockMenu(location, force, Material.CRAFTING_TABLE, net.minecraft.world.level.block.Blocks.CRAFTING_TABLE); }
    @Override public InventoryView openEnchanting(Location location, boolean force) { return openBlockMenu(location, force, Material.ENCHANTING_TABLE, net.minecraft.world.level.block.Blocks.ENCHANTING_TABLE); }
    @Override public InventoryView openAnvil(Location location, boolean force) { return openBlockMenu(location, force, Material.ANVIL, net.minecraft.world.level.block.Blocks.ANVIL); }
    @Override public InventoryView openCartographyTable(Location location, boolean force) { return openBlockMenu(location, force, Material.CARTOGRAPHY_TABLE, net.minecraft.world.level.block.Blocks.CARTOGRAPHY_TABLE); }
    @Override public InventoryView openGrindstone(Location location, boolean force) { return openBlockMenu(location, force, Material.GRINDSTONE, net.minecraft.world.level.block.Blocks.GRINDSTONE); }
    @Override public InventoryView openLoom(Location location, boolean force) { return openBlockMenu(location, force, Material.LOOM, net.minecraft.world.level.block.Blocks.LOOM); }
    @Override public InventoryView openSmithingTable(Location location, boolean force) { return openBlockMenu(location, force, Material.SMITHING_TABLE, net.minecraft.world.level.block.Blocks.SMITHING_TABLE); }
    @Override public InventoryView openStonecutter(Location location, boolean force) { return openBlockMenu(location, force, Material.STONECUTTER, net.minecraft.world.level.block.Blocks.STONECUTTER); }
    @Override public InventoryView openMerchant(org.bukkit.entity.Villager villager, boolean force) { return openMerchant((Merchant) villager, force); }
    @Override public InventoryView openMerchant(Merchant merchant, boolean force) {
        if (merchant == null) throw new IllegalArgumentException("merchant cannot be null");
        if (!force && merchant.isTrading()) return null;
        if (merchant.isTrading() && merchant.getTrader() != null) merchant.getTrader().closeInventory();
        if (merchant instanceof CraftEntity craft && craft.getHandle() instanceof net.minecraft.world.entity.npc.AbstractVillager av) {
            av.setTradingPlayer(getHandle());
            av.openTradingScreen(getHandle(), av.getDisplayName(), av instanceof net.minecraft.world.entity.npc.Villager v ? v.getVillagerData().getLevel() : 1);
            if (getHandle().containerMenu == getHandle().inventoryMenu) return null;
            InventoryView view = getOpenInventory();
            net.minecraft.world.inventory.AbstractContainerMenu menu = getHandle().containerMenu;
            com.mojang.datafixers.util.Pair<net.kyori.adventure.text.Component, net.minecraft.world.inventory.AbstractContainerMenu> result =
                    org.bukkit.craftbukkit.v1_21_R1.event.CraftEventFactory.callInventoryOpenEventWithTitle(
                            getHandle(), menu, view, false);
            if (result.getSecond() == null) {
                getHandle().closeContainer();
                notifyClientClose(menu);
                return null;
            }
            applyTitleOverride(menu, result.getFirst());
            return view;
        }
        throw new IllegalArgumentException("Unsupported merchant implementation: " + merchant.getClass().getName());
    }
    @Override public void closeInventory() {
        closeInventory(InventoryCloseEvent.Reason.PLUGIN);
    }
    @Override public void closeInventory(InventoryCloseEvent.Reason reason) {
        if (reason == null) reason = InventoryCloseEvent.Reason.PLUGIN;
        try {
            if (getHandle().containerMenu == getHandle().inventoryMenu) return;
            org.bukkit.craftbukkit.v1_21_R1.event.CraftEventFactory.handleInventoryCloseEvent(getHandle(), reason);
            net.minecraft.world.inventory.AbstractContainerMenu menu = getHandle().containerMenu;
            notifyClientClose(menu);
            getHandle().closeContainer();
        } catch (Throwable ignored) {
            try { getHandle().closeContainer(); } catch (Throwable ignoredAgain) {}
        }
    }
    @Override public boolean discoverRecipe(NamespacedKey recipe) { return discoverRecipes(java.util.Collections.singleton(recipe)) != 0; }
    @Override public int getEnchantmentSeed() { return getHandle().enchantmentSeed; }
    @Override public void setEnchantmentSeed(int seed) { getHandle().enchantmentSeed = seed; }
    @Override public void setFoodLevel(int value) {
        int clamped = Math.max(0, Math.min(20, value));
        ((io.ampznetwork.lunararc.common.mixin.core.player.FoodDataAccessor) getHandle().getFoodData()).setFoodLevel(clamped);
    }

    // OfflinePlayer
    @Override public boolean isOnline() { return isConnected(); }
    @Override public boolean isBanned() { return org.bukkit.craftbukkit.v1_21_R1.CraftBanList.PROFILE_BANS.isBanned(getPlayerProfile()); }
    @Override public boolean isWhitelisted() { return whitelisted; }
    @Override public void setWhitelisted(boolean value) { this.whitelisted = value; }
    @Override public long getFirstPlayed() { return 0; }

    private Object getNmsStatsCounter() {
        try {
            java.lang.reflect.Method method = getHandle().getClass().getMethod("getStats");
            return method.invoke(getHandle());
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to access the Minecraft ServerStatsCounter for " + getName(), ex);
        }
    }

    private static boolean statisticArgumentMatches(Class<?> parameter, Object argument) {
        if (argument == null) return !parameter.isPrimitive();
        return parameter.isInstance(argument) || parameter.isAssignableFrom(argument.getClass());
    }

    private Object invokeCraftStatistic(String methodName, Object... apiArguments) {
        final Object counter = getNmsStatsCounter();
        final Class<?> craftStatistic;
        try {
            // Paper 1.20.5+ uses the unversioned CraftBukkit implementation package.
            // LunarArc deliberately inherits this helper from the exact Paper runtime selected
            // by libs.versions.toml, so its statistic mapping stays in lockstep with Paper.
            craftStatistic = Class.forName("org.bukkit.craftbukkit.CraftStatistic", true, getClass().getClassLoader());
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("Paper CraftStatistic is missing from the LunarArc runtime", ex);
        }

        java.lang.reflect.Method selected = null;
        outer:
        for (java.lang.reflect.Method candidate : craftStatistic.getDeclaredMethods()) {
            if (!java.lang.reflect.Modifier.isStatic(candidate.getModifiers()) || !candidate.getName().equals(methodName)) continue;
            Class<?>[] parameters = candidate.getParameterTypes();
            if (parameters.length != apiArguments.length + 1 || !statisticArgumentMatches(parameters[0], counter)) continue;
            for (int i = 0; i < apiArguments.length; i++) {
                if (!statisticArgumentMatches(parameters[i + 1], apiArguments[i])) continue outer;
            }
            candidate.trySetAccessible();
            selected = candidate;
            break;
        }
        if (selected == null) {
            throw new IllegalStateException("Paper CraftStatistic does not expose compatible " + methodName + " overload for " + java.util.Arrays.toString(apiArguments));
        }

        Object[] invocation = new Object[apiArguments.length + 1];
        invocation[0] = counter;
        System.arraycopy(apiArguments, 0, invocation, 1, apiArguments.length);
        try {
            return selected.invoke(null, invocation);
        } catch (java.lang.reflect.InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException runtime) throw runtime;
            if (cause instanceof Error error) throw error;
            throw new IllegalStateException("Paper CraftStatistic." + methodName + " failed", cause);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to invoke Paper CraftStatistic." + methodName, ex);
        }
    }

    private int getStatisticFromPaper(Statistic statistic, Object parameter) {
        Object result = parameter == null
            ? invokeCraftStatistic("getStatistic", statistic)
            : invokeCraftStatistic("getStatistic", statistic, parameter);
        if (!(result instanceof Number number)) {
            throw new IllegalStateException("Paper CraftStatistic.getStatistic returned " + (result == null ? "null" : result.getClass().getName()));
        }
        return number.intValue();
    }

    private void mutateStatisticWithPaper(String operation, Statistic statistic, Object parameter, int value) {
        if (value < 0) throw new IllegalArgumentException("Statistic value/amount cannot be negative");
        if (parameter == null) invokeCraftStatistic(operation, statistic, value);
        else invokeCraftStatistic(operation, statistic, parameter, value);
    }

    @Override public int getStatistic(Statistic statistic) { return getStatisticFromPaper(statistic, null); }
    @Override public int getStatistic(Statistic statistic, EntityType entityType) { return getStatisticFromPaper(statistic, java.util.Objects.requireNonNull(entityType, "entityType")); }
    @Override public int getStatistic(Statistic statistic, Material material) { return getStatisticFromPaper(statistic, java.util.Objects.requireNonNull(material, "material")); }
    @Override public void incrementStatistic(Statistic statistic) { incrementStatistic(statistic, 1); }
    @Override public void incrementStatistic(Statistic statistic, int amount) { mutateStatisticWithPaper("incrementStatistic", statistic, null, amount); }
    @Override public void incrementStatistic(Statistic statistic, Material material) { incrementStatistic(statistic, material, 1); }
    @Override public void incrementStatistic(Statistic statistic, Material material, int amount) { mutateStatisticWithPaper("incrementStatistic", statistic, java.util.Objects.requireNonNull(material, "material"), amount); }
    @Override public void incrementStatistic(Statistic statistic, EntityType entityType) { incrementStatistic(statistic, entityType, 1); }
    @Override public void incrementStatistic(Statistic statistic, EntityType entityType, int amount) { mutateStatisticWithPaper("incrementStatistic", statistic, java.util.Objects.requireNonNull(entityType, "entityType"), amount); }
    @Override public void decrementStatistic(Statistic statistic) { decrementStatistic(statistic, 1); }
    @Override public void decrementStatistic(Statistic statistic, int amount) { mutateStatisticWithPaper("decrementStatistic", statistic, null, amount); }
    @Override public void decrementStatistic(Statistic statistic, Material material) { decrementStatistic(statistic, material, 1); }
    @Override public void decrementStatistic(Statistic statistic, Material material, int amount) { mutateStatisticWithPaper("decrementStatistic", statistic, java.util.Objects.requireNonNull(material, "material"), amount); }
    @Override public void decrementStatistic(Statistic statistic, EntityType entityType) { decrementStatistic(statistic, entityType, 1); }
    @Override public void decrementStatistic(Statistic statistic, EntityType entityType, int amount) { mutateStatisticWithPaper("decrementStatistic", statistic, java.util.Objects.requireNonNull(entityType, "entityType"), amount); }
    @Override public void setStatistic(Statistic statistic, int newValue) { mutateStatisticWithPaper("setStatistic", statistic, null, newValue); }
    @Override public void setStatistic(Statistic statistic, Material material, int newValue) { mutateStatisticWithPaper("setStatistic", statistic, java.util.Objects.requireNonNull(material, "material"), newValue); }
    @Override public void setStatistic(Statistic statistic, EntityType entityType, int newValue) { mutateStatisticWithPaper("setStatistic", statistic, java.util.Objects.requireNonNull(entityType, "entityType"), newValue); }
    @Override public Player getPlayer() { return this; }

    // Damageable
    @Override public void setAbsorptionAmount(double amount) {
        if (!Double.isFinite(amount) || amount < 0.0) throw new IllegalArgumentException("Absorption amount must be finite and >= 0");
        getHandle().setAbsorptionAmount((float) amount);
    }
    @Override public void setMaxHealth(double health) { getHandle().getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(health); }
    @Override public void setHealth(double health) { getHandle().setHealth((float) health); }
    @Override public void heal(double amount, EntityRegainHealthEvent.RegainReason reason) { getHandle().heal((float) amount); }
    @Override public double getHealth() { return getHandle().getHealth(); }
    @Override public void damage(double amount) {
        getHandle().hurt(getHandle().level().damageSources().generic(), (float) amount);
    }
    @Override public void damage(double amount, Entity source) {
        getHandle().hurt(getHandle().level().damageSources().generic(), (float) amount);
    }
    @Override public void damage(double amount, DamageSource damageSource) {
        getHandle().hurt(getHandle().level().damageSources().generic(), (float) amount);
    }
    @Override public void resetMaxHealth() { setMaxHealth(20.0); }
    @Override public double getMaxHealth() { return getHandle().getMaxHealth(); }
    @Override public double getAbsorptionAmount() { return getHandle().getAbsorptionAmount(); }

    // Conversable
    @Override public boolean isConversing() { return false; }
    @Override public void acceptConversationInput(String input) {}

    // NetworkClient
    @Override public int getProtocolVersion() { return 0; }

    // Frictional
    @Override public TriState getFrictionState() { return TriState.NOT_SET; }
    @Override public void setFrictionState(TriState state) {}

    // ConfigurationSerializable
    @Override public Map<String, Object> serialize() { return new HashMap<>(); }

    @Override public boolean isSleeping() { return getHandle().isSleeping(); }
    @Override public void setCollidable(boolean collidable) {}
    @Override public boolean isCollidable() { return true; }
    @Override public void setJumping(boolean jumping) {}
    @Override public boolean isJumping() { return false; }
    @Override public void setSwimming(boolean swimming) {}
    @Override public boolean isSwimming() { return getHandle().isSwimming(); }
    @Override public int getActiveItemRemainingTime() { return getHandle().getUseItemRemainingTicks(); }
    @Override public void setNextBeeStingerRemoval(int ticks) {}
    @Override public int getNextBeeStingerRemoval() { return 0; }
    @Override public boolean addPotionEffects(Collection<PotionEffect> effects) {
        boolean result = false;
        for (PotionEffect effect : effects) if (addPotionEffect(effect)) result = true;
        return result;
    }
    @Override public List<Block> getLineOfSight(Set<Material> transparent, int distance) { return Collections.emptyList(); }
    @Override public boolean hasAI() { return false; }
    @Override public void setAI(boolean ai) {}
    @Override public void playPickupItemAnimation(Item item, int quantity) {}
    @Override public ItemStack getItemInUse() { return CraftItemStack.asBukkitCopy(getHandle().getUseItem()); }
    @Override public ItemStack getActiveItem() { return CraftItemStack.asBukkitCopy(getHandle().getUseItem()); }
    @Override public void clearActiveItem() { getHandle().stopUsingItem(); }
    @Override public void playHurtAnimation(float yaw) {}
    @Override public void setRemoveWhenFarAway(boolean remove) {}
    @Override public boolean getRemoveWhenFarAway() { return false; }
    @Override public int getShieldBlockingDelay() { return 0; }
    @Override public void setBeeStingerCooldown(int ticks) {}
    @Override public int getBeeStingerCooldown() { return 0; }
    @Override public @Nullable com.destroystokyo.paper.block.TargetBlockInfo getTargetBlockInfo(int distance, com.destroystokyo.paper.block.TargetBlockInfo.FluidMode fluidMode) { return (com.destroystokyo.paper.block.TargetBlockInfo) java.lang.reflect.Proxy.newProxyInstance(com.destroystokyo.paper.block.TargetBlockInfo.class.getClassLoader(), new Class<?>[] { com.destroystokyo.paper.block.TargetBlockInfo.class }, (p, m, a) -> null); }
    @Override public @Nullable com.destroystokyo.paper.block.TargetBlockInfo getTargetBlockInfo(int distance) { return getTargetBlockInfo(distance, com.destroystokyo.paper.block.TargetBlockInfo.FluidMode.NEVER); }
    @Override public @Nullable com.destroystokyo.paper.entity.TargetEntityInfo getTargetEntityInfo(int distance, boolean nonTransparent) { return (com.destroystokyo.paper.entity.TargetEntityInfo) java.lang.reflect.Proxy.newProxyInstance(com.destroystokyo.paper.entity.TargetEntityInfo.class.getClassLoader(), new Class<?>[] { com.destroystokyo.paper.entity.TargetEntityInfo.class }, (p, m, a) -> null); }
    @Override public int getMaximumNoDamageTicks() { return 0; }
    @Override public void setMaximumNoDamageTicks(int ticks) {}
    @Override public int getActiveItemUsedTime() { return 0; }
    @Override public boolean isLeashed() { return false; }
    @Override public void setRiptiding(boolean riptiding) {}
    @Override public RayTraceResult rayTraceBlocks(double distance) {
        return rayTraceBlocks(distance, FluidCollisionMode.NEVER);
    }
    @Override public RayTraceResult rayTraceBlocks(double distance, FluidCollisionMode fluidMode) {
        return getWorld().rayTraceBlocks(getEyeLocation(), getEyeLocation().getDirection(), distance, fluidMode, false);
    }
    @Override public Location getEyeLocation() {
        Location loc = getLocation();
        loc.setY(loc.getY() + getEyeHeight());
        return loc;
    }
    @Override public void swingMainHand() {}
    @Override public void swingOffHand() {}
    @Override public boolean setLeashHolder(Entity holder) { return false; }
    @Override public void setArrowsStuck(int arrows) {}
    @Override public int getArrowsStuck() { return 0; }
    @Override public boolean clearActivePotionEffects() {
        Collection<PotionEffect> active = getActivePotionEffects();
        if (active.isEmpty()) return false;
        for (PotionEffect e : active) removePotionEffect(e.getType());
        return true;
    }
    @Override public void setActiveItemRemainingTime(int ticks) {}
    @Override public void setLastDamage(double damage) {}
    @Override public double getLastDamage() { return 0; }
    @Override
    public void removePotionEffect(PotionEffectType type) {
        if (type == null) return;
        try {
            net.minecraft.resources.ResourceLocation rl = net.minecraft.resources.ResourceLocation.parse(type.getKey().toString());
            var holder = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getHolder(rl).orElse(null);
            if (holder != null) getHandle().removeEffect(holder);
        } catch (Throwable ignored) {}
    }
    @Override public boolean hasActiveItem() { return getHandle().isUsingItem(); }
    @Override public int getMaximumAir() { return 300; }
    @Override public int getRemainingAir() { return getHandle().getAirSupply(); }
    @Override public void setMaximumAir(int air) {}
    @Override public int getNextArrowRemoval() { return 0; }
    @Override public void startUsingItem(EquipmentSlot slot) {}
    @Override public int getArrowCooldown() { return 0; }
    @Override public int getItemInUseTicks() { return 0; }
    @Override public void setItemInUseTicks(int ticks) {}
    @Override public void setBeeStingersInBody(int count) {}
    @Override public int getBeeStingersInBody() { return 0; }
    @Override public void setCanPickupItems(boolean pickup) {}
    @Override public boolean getCanPickupItems() { return true; }
    @Override public org.bukkit.Sound getEatingSound(ItemStack itemStack) { return org.bukkit.Sound.ENTITY_GENERIC_EAT; }
    @Override public EquipmentSlot getActiveItemHand() { return EquipmentSlot.HAND; }
    @Override
    public boolean addPotionEffect(PotionEffect effect) {
        return addPotionEffect(effect, false);
    }
    @Override
    public boolean addPotionEffect(PotionEffect effect, boolean force) {
        if (effect == null) return false;
        try {
            net.minecraft.resources.ResourceLocation rl = net.minecraft.resources.ResourceLocation.parse(effect.getType().getKey().toString());
            var holder = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getHolder(rl).orElse(null);
            if (holder == null) return false;
            getHandle().addEffect(new net.minecraft.world.effect.MobEffectInstance(
                holder, effect.getDuration(), effect.getAmplifier(), effect.isAmbient(), effect.hasParticles()),
                (net.minecraft.world.entity.Entity) null);
            return true;
        } catch (Throwable t) { return false; }
    }
    @Override
    public PotionEffect getPotionEffect(PotionEffectType type) {
        if (type == null) return null;
        try {
            net.minecraft.resources.ResourceLocation rl = net.minecraft.resources.ResourceLocation.parse(type.getKey().toString());
            var holder = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getHolder(rl).orElse(null);
            if (holder == null) return null;
            net.minecraft.world.effect.MobEffectInstance fx = getHandle().getEffect(holder);
            if (fx == null) return null;
            return new PotionEffect(type, fx.getDuration(), fx.getAmplifier(), fx.isAmbient(), fx.isVisible());
        } catch (Throwable t) { return null; }
    }

    // HumanEntity extra
    @Override public int getFoodLevel() { return getHandle().getFoodData().getFoodLevel(); }

    // OfflinePlayer extra
    @Override public long getLastLogin() { return 0; }
    @Override public long getLastSeen() { return 0; }
    @Override public boolean isConnected() { return getHandle().connection != null; }
    @Override public long getLastPlayed() { return 0; }
    @Override public boolean hasPlayedBefore() { return true; }
    @Override public Location getLastDeathLocation() { return null; }
    @Override @SuppressWarnings("unchecked") public <E extends org.bukkit.BanEntry<? super PlayerProfile>> E ban(String reason, Duration duration, String source) {
        return (E) org.bukkit.craftbukkit.v1_21_R1.CraftBanList.PROFILE_BANS.addBan(getPlayerProfile(), reason, duration, source);
    }
    @Override @SuppressWarnings("unchecked") public <E extends org.bukkit.BanEntry<? super PlayerProfile>> E ban(String reason, Date expires, String source) {
        return (E) org.bukkit.craftbukkit.v1_21_R1.CraftBanList.PROFILE_BANS.addBan(getPlayerProfile(), reason, expires, source);
    }
    @Override @SuppressWarnings("unchecked") public <E extends org.bukkit.BanEntry<? super PlayerProfile>> E ban(String reason, Instant expires, String source) {
        return (E) org.bukkit.craftbukkit.v1_21_R1.CraftBanList.PROFILE_BANS.addBan(getPlayerProfile(), reason, expires, source);
    }

    @Override
    public <E extends org.bukkit.BanEntry<? super PlayerProfile>> E ban(String reason, Duration duration, String source, boolean kickPlayer) {
        E entry = ban(reason, duration, source);
        if (kickPlayer) kick(Component.text(reason == null ? "Banned by an operator." : reason));
        return entry;
    }

    @Override
    public <E extends org.bukkit.BanEntry<? super PlayerProfile>> E ban(String reason, Date expires, String source, boolean kickPlayer) {
        E entry = ban(reason, expires, source);
        if (kickPlayer) kick(Component.text(reason == null ? "Banned by an operator." : reason));
        return entry;
    }

    @Override
    public <E extends org.bukkit.BanEntry<? super PlayerProfile>> E ban(String reason, Instant expires, String source, boolean kickPlayer) {
        E entry = ban(reason, expires, source);
        if (kickPlayer) kick(Component.text(reason == null ? "Banned by an operator." : reason));
        return entry;
    }

    // Damageable extra

    // ProjectileSource extra
    @Override
    public <T extends Projectile> T launchProjectile(Class<? extends T> projectile) {
        return launchProjectile(projectile, null, null);
    }

    @Override
    public <T extends Projectile> T launchProjectile(Class<? extends T> projectile, Vector velocity) {
        return launchProjectile(projectile, velocity, null);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Projectile> T launchProjectile(Class<? extends T> projectile, Vector velocity, Consumer<? super T> function) {
        if (projectile == null) throw new IllegalArgumentException("projectile cannot be null");
        T spawned = (T) getWorld().spawn(getEyeLocation(), projectile);
        if (velocity != null) spawned.setVelocity(velocity);
        if (function != null) function.accept(spawned);
        return spawned;
    }

    // PluginMessageRecipient
    @Override public Set<String> getListeningPluginChannels() { return Collections.emptySet(); }

    // Attributable — delegated to CraftHumanEntity

    // Conversable extra
    @Override public void sendRawMessage(UUID sender, String message) {}
    @Override public InetSocketAddress getVirtualHost() { return getAddress(); }
    @Override public boolean beginConversation(Conversation conversation) { return false; }
    @Override public void abandonConversation(Conversation conversation) {}
    @Override public void abandonConversation(Conversation conversation, ConversationAbandonedEvent details) {}

    @Override public int getArrowsInBody() { return 0; }
    @Override public int getNoActionTicks() { return 0; }

    // Game mode mapping helpers
    private static net.minecraft.world.level.GameType toNMS(GameMode mode) {
        return switch (mode) {
            case SURVIVAL -> net.minecraft.world.level.GameType.SURVIVAL;
            case CREATIVE -> net.minecraft.world.level.GameType.CREATIVE;
            case ADVENTURE -> net.minecraft.world.level.GameType.ADVENTURE;
            case SPECTATOR -> net.minecraft.world.level.GameType.SPECTATOR;
        };
    }

    private static GameMode fromNMS(net.minecraft.world.level.GameType type) {
        return switch (type) {
            case SURVIVAL -> GameMode.SURVIVAL;
            case CREATIVE -> GameMode.CREATIVE;
            case ADVENTURE -> GameMode.ADVENTURE;
            case SPECTATOR -> GameMode.SPECTATOR;
            default -> GameMode.SURVIVAL;
        };
    }
}
