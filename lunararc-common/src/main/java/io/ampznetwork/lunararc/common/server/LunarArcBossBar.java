package io.ampznetwork.lunararc.common.server;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import org.bukkit.NamespacedKey;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.boss.KeyedBossBar;
import org.bukkit.craftbukkit.v1_21_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_21_R1.util.CraftChatMessage;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Bukkit boss bar backed by Minecraft's live {@link ServerBossEvent}.
 * This keeps the plugin-facing API in the shared runtime while leaving packet
 * delivery to vanilla's boss event implementation on every supported loader.
 */
public final class LunarArcBossBar implements KeyedBossBar {
    private final NamespacedKey key;
    private final ServerBossEvent handle;

    private LunarArcBossBar(@Nullable NamespacedKey key, @Nullable String title,
            @NotNull BarColor color, @NotNull BarStyle style, BarFlag... flags) {
        this.key = key != null ? key : NamespacedKey.minecraft("lunararc_bossbar_" + UUID.randomUUID());
        this.handle = new ServerBossEvent(
                toComponent(title),
                toNmsColor(Objects.requireNonNull(color, "color")),
                toNmsStyle(Objects.requireNonNull(style, "style")));
        if (flags != null) {
            for (BarFlag flag : flags) addFlag(flag);
        }
    }

    public static BossBar create(@Nullable String title, @NotNull BarColor color,
            @NotNull BarStyle style, BarFlag... flags) {
        return new LunarArcBossBar(null, title, color, style, flags);
    }

    public static KeyedBossBar createKeyed(@NotNull NamespacedKey key, @Nullable String title,
            @NotNull BarColor color, @NotNull BarStyle style, BarFlag... flags) {
        return new LunarArcBossBar(Objects.requireNonNull(key, "key"), title, color, style, flags);
    }

    public ServerBossEvent getHandle() {
        return handle;
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return key;
    }

    @Override
    public @NotNull String getTitle() {
        return CraftChatMessage.fromComponent(handle.getName());
    }

    @Override
    public void setTitle(@NotNull String title) {
        handle.setName(toComponent(Objects.requireNonNull(title, "title")));
    }

    @Override
    public @NotNull BarColor getColor() {
        return BarColor.valueOf(handle.getColor().name());
    }

    @Override
    public void setColor(@NotNull BarColor color) {
        handle.setColor(toNmsColor(Objects.requireNonNull(color, "color")));
    }

    @Override
    public @NotNull BarStyle getStyle() {
        return fromNmsStyle(handle.getOverlay());
    }

    @Override
    public void setStyle(@NotNull BarStyle style) {
        handle.setOverlay(toNmsStyle(Objects.requireNonNull(style, "style")));
    }

    @Override
    public void removeFlag(@NotNull BarFlag flag) {
        setFlag(Objects.requireNonNull(flag, "flag"), false);
    }

    @Override
    public void addFlag(@NotNull BarFlag flag) {
        setFlag(Objects.requireNonNull(flag, "flag"), true);
    }

    @Override
    public boolean hasFlag(@NotNull BarFlag flag) {
        return switch (Objects.requireNonNull(flag, "flag")) {
            case DARKEN_SKY -> handle.shouldDarkenScreen();
            case PLAY_BOSS_MUSIC -> handle.shouldPlayBossMusic();
            case CREATE_FOG -> handle.shouldCreateWorldFog();
        };
    }

    @Override
    public void setProgress(double progress) {
        if (!Double.isFinite(progress) || progress < 0.0D || progress > 1.0D) {
            throw new IllegalArgumentException("Progress must be between 0.0 and 1.0 (" + progress + ")");
        }
        handle.setProgress((float) progress);
    }

    @Override
    public double getProgress() {
        return handle.getProgress();
    }

    @Override
    public void addPlayer(@NotNull Player player) {
        Objects.requireNonNull(player, "player");
        if (!(player instanceof CraftPlayer craftPlayer)) {
            throw new IllegalArgumentException("Player is not a LunarArc CraftPlayer: " + player.getClass().getName());
        }
        ServerPlayer serverPlayer = craftPlayer.getHandle();
        if (serverPlayer.connection == null) {
            throw new IllegalArgumentException("player is not fully connected (wait for PlayerJoinEvent)");
        }
        handle.addPlayer(serverPlayer);
    }

    @Override
    public void removePlayer(@NotNull Player player) {
        Objects.requireNonNull(player, "player");
        if (player instanceof CraftPlayer craftPlayer) handle.removePlayer(craftPlayer.getHandle());
    }

    @Override
    public void removeAll() {
        for (ServerPlayer player : new ArrayList<>(handle.getPlayers())) handle.removePlayer(player);
    }

    @Override
    public @NotNull List<Player> getPlayers() {
        List<Player> players = new ArrayList<>(handle.getPlayers().size());
        for (ServerPlayer player : handle.getPlayers()) {
            org.bukkit.entity.Entity bukkit = player.getBukkitEntity();
            if (bukkit instanceof Player p) players.add(p);
        }
        return Collections.unmodifiableList(players);
    }

    @Override
    public void setVisible(boolean visible) {
        handle.setVisible(visible);
    }

    @Override
    public boolean isVisible() {
        return handle.isVisible();
    }

    @Override
    public void show() {
        setVisible(true);
    }

    @Override
    public void hide() {
        setVisible(false);
    }

    private void setFlag(BarFlag flag, boolean enabled) {
        switch (flag) {
            case DARKEN_SKY -> handle.setDarkenScreen(enabled);
            case PLAY_BOSS_MUSIC -> handle.setPlayBossMusic(enabled);
            case CREATE_FOG -> handle.setCreateWorldFog(enabled);
        }
    }

    private static Component toComponent(@Nullable String title) {
        Component component = CraftChatMessage.fromStringOrNull(title == null ? "" : title);
        return component == null ? Component.empty() : component;
    }

    private static BossEvent.BossBarColor toNmsColor(BarColor color) {
        return BossEvent.BossBarColor.valueOf(color.name());
    }

    private static BossEvent.BossBarOverlay toNmsStyle(BarStyle style) {
        return switch (style) {
            case SOLID -> BossEvent.BossBarOverlay.PROGRESS;
            case SEGMENTED_6 -> BossEvent.BossBarOverlay.NOTCHED_6;
            case SEGMENTED_10 -> BossEvent.BossBarOverlay.NOTCHED_10;
            case SEGMENTED_12 -> BossEvent.BossBarOverlay.NOTCHED_12;
            case SEGMENTED_20 -> BossEvent.BossBarOverlay.NOTCHED_20;
        };
    }

    private static BarStyle fromNmsStyle(BossEvent.BossBarOverlay style) {
        return switch (style) {
            case PROGRESS -> BarStyle.SOLID;
            case NOTCHED_6 -> BarStyle.SEGMENTED_6;
            case NOTCHED_10 -> BarStyle.SEGMENTED_10;
            case NOTCHED_12 -> BarStyle.SEGMENTED_12;
            case NOTCHED_20 -> BarStyle.SEGMENTED_20;
        };
    }
}
