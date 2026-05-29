package org.bukkit.craftbukkit.v1_21_R1;

import org.bukkit.BanEntry;
import org.bukkit.BanList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Minimal in-memory BanList used as a stand-in for NeoForge/Fabric environments.
 * All bans are runtime-only and do not persist to disk.
 */
@SuppressWarnings({"rawtypes", "unchecked", "deprecation"})
public class CraftBanList<T> implements BanList<T> {

    public static final CraftBanList<String> NAME_BANS = new CraftBanList<>();
    public static final CraftBanList<java.net.InetAddress> IP_BANS = new CraftBanList<>();

    private final Map<String, Entry<T>> entries = new ConcurrentHashMap<>();

    @Override
    public @Nullable BanEntry<T> getBanEntry(@NotNull T target) {
        return entries.get(key(target));
    }

    @Override
    public @Nullable BanEntry<T> addBan(@NotNull T target, @Nullable String reason,
                                        @Nullable Date expires, @Nullable String source) {
        Entry<T> e = new Entry<>(target, reason, expires, source == null ? "LunarArc" : source);
        entries.put(key(target), e);
        return e;
    }

    @Override
    public @Nullable BanEntry<T> addBan(@NotNull T target, @Nullable String reason,
                                        @Nullable Duration duration, @Nullable String source) {
        Date exp = duration == null ? null : Date.from(Instant.now().plus(duration));
        return addBan(target, reason, exp, source);
    }

    @Override
    public @Nullable BanEntry<T> addBan(@NotNull T target, @Nullable String reason,
                                        @Nullable Instant expires, @Nullable String source) {
        Date exp = expires == null ? null : Date.from(expires);
        return addBan(target, reason, exp, source);
    }

    @Override
    public @NotNull Set<BanEntry<T>> getBanEntries() {
        return entries.values().stream().collect(Collectors.toSet());
    }

    @Override
    public boolean isBanned(@NotNull T target) {
        Entry<T> e = entries.get(key(target));
        if (e == null) return false;
        if (e.expiration != null && e.expiration.before(new Date())) {
            entries.remove(key(target));
            return false;
        }
        return true;
    }

    @Override
    public void pardon(@NotNull T target) {
        entries.remove(key(target));
    }

    private String key(T target) {
        return target == null ? "" : target.toString();
    }

    private static final class Entry<T> implements BanEntry<T> {
        private final T target;
        private String reason;
        private Date expiration;
        private String source;
        private final Date created = new Date();

        Entry(T target, String reason, Date expiration, String source) {
            this.target = target;
            this.reason = reason;
            this.expiration = expiration;
            this.source = source;
        }

        @Override public @NotNull T getTarget() { return target; }
        @Override public @NotNull Date getCreated() { return created; }
        @Override public @NotNull String getSource() { return source == null ? "LunarArc" : source; }
        @Override public @Nullable Date getExpiration() { return expiration; }
        @Override public @Nullable String getReason() { return reason; }
        @Override public void setReason(@Nullable String reason) { this.reason = reason; }
        @Override public void setSource(@NotNull String source) { this.source = source; }
        @Override public void setExpiration(@Nullable Date expiration) { this.expiration = expiration; }
        @Override public void save() {}
        @Override public @NotNull BanList.Type getType() {
            return target instanceof java.net.InetAddress ? BanList.Type.IP : BanList.Type.NAME;
        }
    }
}
