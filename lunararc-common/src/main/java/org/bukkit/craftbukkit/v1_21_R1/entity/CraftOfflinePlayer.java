package org.bukkit.craftbukkit.v1_21_R1.entity;

import org.bukkit.BanEntry;
import org.bukkit.BanList;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.profile.PlayerProfile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class CraftOfflinePlayer implements OfflinePlayer {
    private final UUID uuid;
    private final String name;
    private final org.bukkit.craftbukkit.v1_21_R1.persistence.CraftPersistentDataContainer persistentDataContainer = new org.bukkit.craftbukkit.v1_21_R1.persistence.CraftPersistentDataContainer();

    public CraftOfflinePlayer(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    @Override public @NotNull UUID getUniqueId() { return uuid; }
    @Override public @Nullable String getName() { return name; }
    @Override public boolean isOnline() {
        return org.bukkit.Bukkit.getPlayer(uuid) != null;
    }
    @Override public boolean isConnected() { return isOnline(); }
    @Override public @Nullable Player getPlayer() { return org.bukkit.Bukkit.getPlayer(uuid); }
    @Override public boolean hasPlayedBefore() {
        Player player = getPlayer();
        if (player != null) return player.hasPlayedBefore();
        return getFirstPlayed() > 0L || getLastPlayed() > 0L;
    }
    @Override public boolean isBanned() {
        return org.bukkit.craftbukkit.v1_21_R1.CraftBanList.PROFILE_BANS.isBanned(getPlayerProfile());
    }
    @Override public boolean isWhitelisted() {
        Player player = getPlayer();
        if (player != null) return player.isWhitelisted();
        try {
            Object list = org.bukkit.Bukkit.getServer().getClass().getMethod("getHandle").invoke(org.bukkit.Bukkit.getServer());
            Object playerList = list.getClass().getMethod("getPlayerList").invoke(list);
            Object whiteList = playerList.getClass().getMethod("getWhiteList").invoke(playerList);
            Object result = whiteList.getClass().getMethod("isWhiteListed", com.mojang.authlib.GameProfile.class)
                    .invoke(whiteList, gameProfile());
            return result instanceof Boolean b && b;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }
    @Override public void setWhitelisted(boolean value) {
        Player player = getPlayer();
        if (player != null) { player.setWhitelisted(value); return; }
        mutateStoredUserList("getWhiteList", "net.minecraft.server.players.UserWhiteListEntry", value);
    }
    @Override public boolean isOp() {
        Player player = getPlayer();
        if (player != null) return player.isOp();
        try {
            Object server = org.bukkit.Bukkit.getServer().getClass().getMethod("getHandle").invoke(org.bukkit.Bukkit.getServer());
            Object playerList = server.getClass().getMethod("getPlayerList").invoke(server);
            Object result = playerList.getClass().getMethod("isOp", com.mojang.authlib.GameProfile.class).invoke(playerList, gameProfile());
            return result instanceof Boolean b && b;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }
    @Override public void setOp(boolean value) {
        Player player = getPlayer();
        if (player != null) { player.setOp(value); return; }
        try {
            Object server = org.bukkit.Bukkit.getServer().getClass().getMethod("getHandle").invoke(org.bukkit.Bukkit.getServer());
            Object playerList = server.getClass().getMethod("getPlayerList").invoke(server);
            if (value) {
                Class<?> entryClass = Class.forName("net.minecraft.server.players.ServerOpListEntry");
                Object entry = null;
                for (var ctor : entryClass.getConstructors()) {
                    Class<?>[] t = ctor.getParameterTypes();
                    if (t.length == 4 && t[0] == com.mojang.authlib.GameProfile.class) {
                        entry = ctor.newInstance(gameProfile(), 4, false, false);
                        break;
                    }
                }
                if (entry == null) throw new ReflectiveOperationException("No ServerOpListEntry constructor");
                Object ops = playerList.getClass().getMethod("getOps").invoke(playerList);
                ops.getClass().getMethod("add", entryClass).invoke(ops, entry);
            } else {
                Object ops = playerList.getClass().getMethod("getOps").invoke(playerList);
                ops.getClass().getMethod("remove", com.mojang.authlib.GameProfile.class).invoke(ops, gameProfile());
            }
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to update operator state for " + uuid, ex);
        }
    }
    @Override public @Nullable Location getBedSpawnLocation() { return getRespawnLocation(); }
    @Override public @Nullable Location getRespawnLocation() {
        Player player = getPlayer();
        return player == null ? null : player.getRespawnLocation();
    }
    public @Nullable Location getRespawnLocation(boolean loadLocationAndValidate) { return null; }
    @Override public long getFirstPlayed() { Player p=getPlayer(); return p == null ? 0L : p.getFirstPlayed(); }
    @Override public long getLastPlayed() { Player p=getPlayer(); return p == null ? 0L : p.getLastPlayed(); }
    @Override public long getLastLogin() { Player p=getPlayer(); return p == null ? getLastPlayed() : p.getLastLogin(); }
    @Override public long getLastSeen() { Player p=getPlayer(); return p == null ? getLastPlayed() : p.getLastSeen(); }
    @Override public @Nullable Location getLastDeathLocation() { Player p=getPlayer(); return p == null ? null : p.getLastDeathLocation(); }
    @Override public @Nullable Location getLocation() { Player p=getPlayer(); return p == null ? null : p.getLocation(); }
    @Override public void incrementStatistic(@NotNull Statistic statistic) { requireOnlinePlayer().incrementStatistic(statistic); }
    @Override public void decrementStatistic(@NotNull Statistic statistic) { requireOnlinePlayer().decrementStatistic(statistic); }
    @Override public void incrementStatistic(@NotNull Statistic statistic, int amount) { requireOnlinePlayer().incrementStatistic(statistic, amount); }
    @Override public void decrementStatistic(@NotNull Statistic statistic, int amount) { requireOnlinePlayer().decrementStatistic(statistic, amount); }
    @Override public void setStatistic(@NotNull Statistic statistic, int newValue) { requireOnlinePlayer().setStatistic(statistic, newValue); }
    @Override public int getStatistic(@NotNull Statistic statistic) { return requireOnlinePlayer().getStatistic(statistic); }
    @Override public void incrementStatistic(@NotNull Statistic statistic, @NotNull Material material) { requireOnlinePlayer().incrementStatistic(statistic, material); }
    @Override public void decrementStatistic(@NotNull Statistic statistic, @NotNull Material material) { requireOnlinePlayer().decrementStatistic(statistic, material); }
    @Override public int getStatistic(@NotNull Statistic statistic, @NotNull Material material) { return requireOnlinePlayer().getStatistic(statistic, material); }
    @Override public void incrementStatistic(@NotNull Statistic statistic, @NotNull Material material, int amount) { requireOnlinePlayer().incrementStatistic(statistic, material, amount); }
    @Override public void decrementStatistic(@NotNull Statistic statistic, @NotNull Material material, int amount) { requireOnlinePlayer().decrementStatistic(statistic, material, amount); }
    @Override public void setStatistic(@NotNull Statistic statistic, @NotNull Material material, int newValue) { requireOnlinePlayer().setStatistic(statistic, material, newValue); }
    @Override public int getStatistic(@NotNull Statistic statistic, @NotNull EntityType entityType) { return requireOnlinePlayer().getStatistic(statistic, entityType); }
    @Override public void incrementStatistic(@NotNull Statistic statistic, @NotNull EntityType entityType) { requireOnlinePlayer().incrementStatistic(statistic, entityType); }
    @Override public void decrementStatistic(@NotNull Statistic statistic, @NotNull EntityType entityType) { requireOnlinePlayer().decrementStatistic(statistic, entityType); }
    @Override public void incrementStatistic(@NotNull Statistic statistic, @NotNull EntityType entityType, int amount) { requireOnlinePlayer().incrementStatistic(statistic, entityType, amount); }
    @Override public void decrementStatistic(@NotNull Statistic statistic, @NotNull EntityType entityType, int amount) { requireOnlinePlayer().decrementStatistic(statistic, entityType, amount); }
    @Override public void setStatistic(@NotNull Statistic statistic, @NotNull EntityType entityType, int newValue) { requireOnlinePlayer().setStatistic(statistic, entityType, newValue); }
    @Override public @NotNull Map<String, Object> serialize() { return java.util.Collections.singletonMap("uuid", uuid.toString()); }

    @Override
    public @Nullable BanEntry<PlayerProfile> ban(@Nullable String reason, @Nullable Date expires, @Nullable String source) {
        return castBanEntry(org.bukkit.craftbukkit.v1_21_R1.CraftBanList.PROFILE_BANS.addBan(getPlayerProfile(), reason, expires, source));
    }

    @Override
    @SuppressWarnings("deprecation")
    public @Nullable BanEntry<PlayerProfile> ban(@Nullable String reason, @Nullable Duration duration, @Nullable String source) {
        return castBanEntry(org.bukkit.craftbukkit.v1_21_R1.CraftBanList.PROFILE_BANS.addBan(getPlayerProfile(), reason, duration, source));
    }

    @Override
    @SuppressWarnings("deprecation")
    public @Nullable BanEntry<PlayerProfile> ban(@Nullable String reason, @Nullable Instant expires, @Nullable String source) {
        return castBanEntry(org.bukkit.craftbukkit.v1_21_R1.CraftBanList.PROFILE_BANS.addBan(getPlayerProfile(), reason, expires, source));
    }

    @Override
    public @NotNull com.destroystokyo.paper.profile.PlayerProfile getPlayerProfile() {
        return new io.ampznetwork.lunararc.common.server.LunarArcPlayerProfile(uuid, name);
    }

    @Override
    public @NotNull PersistentDataContainer getPersistentDataContainer() {
        Player player = getPlayer();
        return player == null ? persistentDataContainer : player.getPersistentDataContainer();
    }

    private Player requireOnlinePlayer() {
        Player player = getPlayer();
        if (player == null) throw new IllegalStateException("Offline statistics are not loaded for " + uuid);
        return player;
    }

    private com.mojang.authlib.GameProfile gameProfile() {
        return new com.mojang.authlib.GameProfile(uuid, name == null ? "" : name);
    }

    private void mutateStoredUserList(String accessor, String entryClassName, boolean add) {
        try {
            Object server = org.bukkit.Bukkit.getServer().getClass().getMethod("getHandle").invoke(org.bukkit.Bukkit.getServer());
            Object playerList = server.getClass().getMethod("getPlayerList").invoke(server);
            Object list = playerList.getClass().getMethod(accessor).invoke(playerList);
            if (add) {
                Class<?> entryClass = Class.forName(entryClassName);
                Object entry = entryClass.getConstructor(com.mojang.authlib.GameProfile.class).newInstance(gameProfile());
                list.getClass().getMethod("add", entryClass).invoke(list, entry);
            } else {
                list.getClass().getMethod("remove", com.mojang.authlib.GameProfile.class).invoke(list, gameProfile());
            }
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to update stored player list for " + uuid, ex);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BanEntry<PlayerProfile> castBanEntry(BanEntry entry) {
        return (BanEntry<PlayerProfile>) entry;
    }
}

