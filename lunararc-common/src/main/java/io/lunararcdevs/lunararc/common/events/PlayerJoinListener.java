package io.lunararcdevs.lunararc.common.events;

import com.destroystokyo.paper.PaperVersionFetcher;
import io.lunararcdevs.lunararc.common.server.LunarArcVersionInfo;
import io.lunararcdevs.lunararc.i18n.TranslationManager;
import com.mojang.authlib.GameProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class PlayerJoinListener {
    private static final long NOTIFY_WINDOW_MILLIS = 60_000L;
    private static final java.util.Map<java.util.UUID, Long> LAST_NOTIFIED = new java.util.concurrent.ConcurrentHashMap<>();

    private PlayerJoinListener() {
    }

    private static boolean claimNotification(java.util.UUID playerId) {
        long now = System.currentTimeMillis();
        boolean[] claimed = {false};
        LAST_NOTIFIED.compute(playerId, (id, previous) -> {
            if (previous == null || now - previous > NOTIFY_WINDOW_MILLIS) {
                claimed[0] = true;
                return now;
            }
            return previous;
        });
        return claimed[0];
    }

    public static void checkAndNotify(Player player, GameProfile profile, net.minecraft.server.MinecraftServer server,
                                      Consumer<Runnable> serverExecutor) {
        if (!hasLevelFourOperatorAccess(profile, server)) return;
        if (!claimNotification(player.getUniqueId())) return;

        CompletableFuture
                .supplyAsync(PaperVersionFetcher::fetchLatestRelease)
                .thenAccept(release -> release.ifPresent(latest -> {
                    if (!isCurrentVersion(latest.version())) {
                        serverExecutor.accept(() -> notifyPlayer(player, latest));
                    }
                }));
    }

    private static boolean hasLevelFourOperatorAccess(GameProfile profile, net.minecraft.server.MinecraftServer server) {
        return server.getProfilePermissions(profile) >= 4;
    }

    private static boolean isCurrentVersion(String latestVersion) {
        return PaperVersionFetcher.isSameVersion(LunarArcVersionInfo.lunarArcVersion(), latestVersion);
    }

    private static void notifyPlayer(Player player, PaperVersionFetcher.Release release) {
        if (!player.isOnline()) return;
        player.sendMessage(Component.text("[LunarArc]", NamedTextColor.AQUA, TextDecoration.BOLD));
        player.sendMessage(Component.text(TranslationManager.get("ingame.update.available"), NamedTextColor.YELLOW));
        player.sendMessage(Component.text(TranslationManager.get("ingame.update.current"), NamedTextColor.GRAY)
                .append(Component.text(LunarArcVersionInfo.lunarArcVersion(), NamedTextColor.YELLOW))
                .append(Component.text(" → ", NamedTextColor.DARK_GRAY))
                .append(Component.text(TranslationManager.get("ingame.update.new"), NamedTextColor.GRAY))
                .append(Component.text(release.version(), NamedTextColor.GREEN)));
        player.sendMessage(Component.text(TranslationManager.get("ingame.update.download_link"),
                        NamedTextColor.GOLD, TextDecoration.UNDERLINED)
                .clickEvent(ClickEvent.openUrl(release.downloadUrl())));
    }
}
