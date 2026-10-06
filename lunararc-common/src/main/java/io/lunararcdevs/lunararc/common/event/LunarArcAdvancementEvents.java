package io.lunararcdevs.lunararc.common.event;

import com.destroystokyo.paper.event.player.PlayerAdvancementCriterionGrantEvent;
import io.lunararcdevs.lunararc.common.LunarArcServerAccess;
import io.lunararcdevs.lunararc.common.bridge.EntityBridge;
import io.lunararcdevs.lunararc.common.messaging.LunarArcComponentPipeline;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.craftbukkit.advancement.CraftAdvancement;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;

public final class LunarArcAdvancementEvents {
    public record Announcement(PlayerAdvancementDoneEvent event, net.kyori.adventure.text.Component original) {}

    private static final ThreadLocal<Announcement> ANNOUNCING = new ThreadLocal<>();

    private LunarArcAdvancementEvents() {}

    public static Announcement fireDone(ServerPlayer player, AdvancementHolder advancement) {
        if (PlayerAdvancementDoneEvent.getHandlerList().getRegisteredListeners().length == 0) return null;
        org.bukkit.entity.Player bukkitPlayer = bukkitPlayer(player);
        if (bukkitPlayer == null) return null;

        net.kyori.adventure.text.Component message = advancement.value().display()
                .filter(DisplayInfo::shouldAnnounceChat)
                .map(info -> LunarArcComponentPipeline.toAdventure(info.getType().createAnnouncement(advancement, player)))
                .orElse(null);
        PlayerAdvancementDoneEvent event =
                new PlayerAdvancementDoneEvent(bukkitPlayer, new CraftAdvancement(advancement), message);
        LunarArcServerAccess.getCraftServer(player.server).getPluginManager().callEvent(event);
        return new Announcement(event, message);
    }

    public static boolean allowCriterion(ServerPlayer player, AdvancementHolder advancement, String criterion) {
        if (PlayerAdvancementCriterionGrantEvent.getHandlerList().getRegisteredListeners().length == 0) return true;
        org.bukkit.entity.Player bukkitPlayer = bukkitPlayer(player);
        if (bukkitPlayer == null) return true;

        PlayerAdvancementCriterionGrantEvent event =
                new PlayerAdvancementCriterionGrantEvent(bukkitPlayer, new CraftAdvancement(advancement), criterion);
        LunarArcServerAccess.getCraftServer(player.server).getPluginManager().callEvent(event);
        return !event.isCancelled();
    }

    public static void begin(Announcement announcement) {
        if (announcement != null) ANNOUNCING.set(announcement);
    }

    public static void end() {
        ANNOUNCING.remove();
    }

    public static boolean suppressed() {
        Announcement announcement = ANNOUNCING.get();
        return announcement != null && announcement.event().message() == null;
    }

    public static net.minecraft.network.chat.Component rewrite(net.minecraft.network.chat.Component vanilla) {
        Announcement announcement = ANNOUNCING.get();
        if (announcement == null) return vanilla;
        net.kyori.adventure.text.Component message = announcement.event().message();
        if (message == null || message.equals(announcement.original())) return vanilla;
        return LunarArcComponentPipeline.fromAdventure(message);
    }

    private static org.bukkit.entity.Player bukkitPlayer(ServerPlayer player) {
        return player != null && ((EntityBridge) player).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Player bukkit
                ? bukkit
                : null;
    }
}
