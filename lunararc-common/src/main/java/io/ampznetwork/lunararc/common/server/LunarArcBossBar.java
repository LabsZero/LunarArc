package io.ampznetwork.lunararc.common.server;

import org.bukkit.NamespacedKey;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.boss.KeyedBossBar;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * In-memory BossBar implementation. Does not send actual boss bar packets
 * (that requires Mixin/NMS support), but satisfies the API contract
 * so plugins that store/mutate boss bars don't crash.
 */
public class LunarArcBossBar implements KeyedBossBar {

    private final NamespacedKey key;
    private String title;
    private BarColor color;
    private BarStyle style;
    private double progress = 1.0;
    private boolean visible = true;
    private final Set<BarFlag> flags = EnumSet.noneOf(BarFlag.class);
    private final List<Player> players = new ArrayList<>();

    private LunarArcBossBar(@Nullable NamespacedKey key, @Nullable String title,
            @NotNull BarColor color, @NotNull BarStyle style, BarFlag... flags) {
        this.key = key != null ? key : NamespacedKey.minecraft("lunararc_bossbar_" + System.nanoTime());
        this.title = title != null ? title : "";
        this.color = color;
        this.style = style;
        if (flags != null) this.flags.addAll(Arrays.asList(flags));
    }

    public static BossBar create(@Nullable String title, @NotNull BarColor color,
            @NotNull BarStyle style, BarFlag... flags) {
        return new LunarArcBossBar(null, title, color, style, flags);
    }

    public static KeyedBossBar createKeyed(@NotNull NamespacedKey key, @Nullable String title,
            @NotNull BarColor color, @NotNull BarStyle style, BarFlag... flags) {
        return new LunarArcBossBar(key, title, color, style, flags);
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return key;
    }

    @Override
    public @NotNull String getTitle() {
        return title;
    }

    @Override
    public void setTitle(@NotNull String title) {
        this.title = title;
    }

    @Override
    public @NotNull BarColor getColor() {
        return color;
    }

    @Override
    public void setColor(@NotNull BarColor color) {
        this.color = color;
    }

    @Override
    public @NotNull BarStyle getStyle() {
        return style;
    }

    @Override
    public void setStyle(@NotNull BarStyle style) {
        this.style = style;
    }

    @Override
    public void removeFlag(@NotNull BarFlag flag) {
        flags.remove(flag);
    }

    @Override
    public void addFlag(@NotNull BarFlag flag) {
        flags.add(flag);
    }

    @Override
    public boolean hasFlag(@NotNull BarFlag flag) {
        return flags.contains(flag);
    }

    @Override
    public void setProgress(double progress) {
        if (progress < 0.0 || progress > 1.0)
            throw new IllegalArgumentException("progress must be between 0 and 1");
        this.progress = progress;
    }

    @Override
    public double getProgress() {
        return progress;
    }

    @Override
    public void addPlayer(@NotNull Player player) {
        if (!players.contains(player)) players.add(player);
    }

    @Override
    public void removePlayer(@NotNull Player player) {
        players.remove(player);
    }

    @Override
    public void removeAll() {
        players.clear();
    }

    @Override
    public @NotNull List<Player> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public void show() {
        this.visible = true;
    }

    @Override
    public void hide() {
        this.visible = false;
    }
}
