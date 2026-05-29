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

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

public class CraftOfflinePlayer implements OfflinePlayer {
    private final UUID uuid;
    private final String name;

    public CraftOfflinePlayer(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    @Override public @NotNull UUID getUniqueId() { return uuid; }
    @Override public @Nullable String getName() { return name; }
    @Override public boolean isOnline() {
        return org.bukkit.Bukkit.getPlayer(uuid) != null;
    }
    @Override public @Nullable Player getPlayer() { return org.bukkit.Bukkit.getPlayer(uuid); }
    @Override public boolean hasPlayedBefore() { return false; }
    @Override public boolean isBanned() { return false; }
    @Override public boolean isWhitelisted() { return false; }
    @Override public void setWhitelisted(boolean value) {}
    @Override public boolean isOp() { return false; }
    @Override public void setOp(boolean value) {}
    @Override public @Nullable Location getBedSpawnLocation() { return null; }
    @Override public @Nullable Location getRespawnLocation() { return null; }
    @Override public long getFirstPlayed() { return 0; }
    @Override public long getLastPlayed() { return 0; }
    @Override public long getLastLogin() { return 0; }
    @Override public long getLastSeen() { return 0; }
    @Override public @Nullable Location getLastDeathLocation() { return null; }
    @Override public void incrementStatistic(@NotNull Statistic statistic) {}
    @Override public void decrementStatistic(@NotNull Statistic statistic) {}
    @Override public void incrementStatistic(@NotNull Statistic statistic, int amount) {}
    @Override public void decrementStatistic(@NotNull Statistic statistic, int amount) {}
    @Override public void setStatistic(@NotNull Statistic statistic, int newValue) {}
    @Override public int getStatistic(@NotNull Statistic statistic) { return 0; }
    @Override public void incrementStatistic(@NotNull Statistic statistic, @NotNull Material material) {}
    @Override public void decrementStatistic(@NotNull Statistic statistic, @NotNull Material material) {}
    @Override public int getStatistic(@NotNull Statistic statistic, @NotNull Material material) { return 0; }
    @Override public void incrementStatistic(@NotNull Statistic statistic, @NotNull Material material, int amount) {}
    @Override public void decrementStatistic(@NotNull Statistic statistic, @NotNull Material material, int amount) {}
    @Override public void setStatistic(@NotNull Statistic statistic, @NotNull Material material, int newValue) {}
    @Override public int getStatistic(@NotNull Statistic statistic, @NotNull EntityType entityType) { return 0; }
    @Override public void incrementStatistic(@NotNull Statistic statistic, @NotNull EntityType entityType) {}
    @Override public void decrementStatistic(@NotNull Statistic statistic, @NotNull EntityType entityType) {}
    @Override public void incrementStatistic(@NotNull Statistic statistic, @NotNull EntityType entityType, int amount) {}
    @Override public void decrementStatistic(@NotNull Statistic statistic, @NotNull EntityType entityType, int amount) {}
    @Override public void setStatistic(@NotNull Statistic statistic, @NotNull EntityType entityType, int newValue) {}
    @Override public @NotNull Map<String, Object> serialize() { return java.util.Collections.singletonMap("uuid", uuid.toString()); }

    @Override
    public @Nullable BanEntry<PlayerProfile> ban(@Nullable String reason, @Nullable Date expires, @Nullable String source) {
        return null;
    }

    @Override
    @SuppressWarnings("deprecation")
    public @Nullable BanEntry<PlayerProfile> ban(@Nullable String reason, @Nullable Duration duration, @Nullable String source) {
        return null;
    }

    @Override
    @SuppressWarnings("deprecation")
    public @Nullable BanEntry<PlayerProfile> ban(@Nullable String reason, @Nullable Instant expires, @Nullable String source) {
        return null;
    }

    @Override
    public @NotNull com.destroystokyo.paper.profile.PlayerProfile getPlayerProfile() {
        return new io.ampznetwork.lunararc.common.server.LunarArcPlayerProfile(uuid, name);
    }

    @Override
    public @NotNull PersistentDataContainer getPersistentDataContainer() {
        return (PersistentDataContainer) Proxy.newProxyInstance(
            PersistentDataContainer.class.getClassLoader(),
            new Class<?>[]{ PersistentDataContainer.class },
            (proxy, method, args) -> {
                if (method.getReturnType() == boolean.class) return false;
                if (method.getReturnType() == int.class) return 0;
                if (method.getName().equals("getAdapterContext")) return null;
                return null;
            });
    }
}
