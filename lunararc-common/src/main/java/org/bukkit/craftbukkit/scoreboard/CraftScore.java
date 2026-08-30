package org.bukkit.craftbukkit.scoreboard;

import com.google.common.base.Preconditions;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;

final class CraftScore implements Score {
    private final ScoreHolder entry;
    private final CraftObjective objective;

    CraftScore(CraftObjective objective, ScoreHolder entry) {
        this.objective = java.util.Objects.requireNonNull(objective, "objective");
        this.entry = java.util.Objects.requireNonNull(entry, "entry");
    }

    @Override
    public OfflinePlayer getPlayer() {
        return Bukkit.getOfflinePlayer(this.entry.getScoreboardName());
    }

    @Override
    public String getEntry() {
        return this.entry.getScoreboardName();
    }

    @Override
    public Objective getObjective() {
        return this.objective;
    }

    @Override
    public int getScore() {
        Scoreboard board = this.objective.checkState().board;
        ReadOnlyScoreInfo info = board.getPlayerScoreInfo(this.entry, this.objective.getHandle());
        return info == null ? 0 : info.value();
    }

    @Override
    public void setScore(int score) {
        this.objective.checkState().board.getOrCreatePlayerScore(this.entry, this.objective.getHandle()).set(score);
    }

    @Override
    public boolean isScoreSet() {
        return this.objective.checkState().board.getPlayerScoreInfo(this.entry, this.objective.getHandle()) != null;
    }

    @Override
    public CraftScoreboard getScoreboard() {
        return this.objective.getScoreboard();
    }

    public void resetScore() {
        this.objective.checkState().board.resetSinglePlayerScore(this.entry, this.objective.getHandle());
    }

    public io.papermc.paper.scoreboard.numbers.NumberFormat numberFormat() {
        ReadOnlyScoreInfo info = this.objective.checkState().board.getPlayerScoreInfo(this.entry, this.objective.getHandle());
        if (info == null || info.numberFormat() == null) return null;
        return io.papermc.paper.util.PaperScoreboardFormat.asPaper(info.numberFormat());
    }

    public void numberFormat(io.papermc.paper.scoreboard.numbers.NumberFormat format) {
        net.minecraft.world.scores.ScoreAccess access = this.objective.checkState().board
                .getOrCreatePlayerScore(this.entry, this.objective.getHandle());
        access.numberFormatOverride(format == null ? null : io.papermc.paper.util.PaperScoreboardFormat.asVanilla(format));
    }

    public boolean isTriggerable() {
        if (!this.objective.getTrackedCriteria().equals(org.bukkit.scoreboard.Criteria.TRIGGER)) return false;
        ReadOnlyScoreInfo info = this.objective.checkState().board.getPlayerScoreInfo(this.entry, this.objective.getHandle());
        return info != null && !info.isLocked();
    }

    public void setTriggerable(boolean triggerable) {
        Preconditions.checkArgument(this.objective.getTrackedCriteria().equals(org.bukkit.scoreboard.Criteria.TRIGGER),
                "the criteria isn't 'trigger'");
        net.minecraft.world.scores.ScoreAccess score = this.objective.checkState().board
                .getOrCreatePlayerScore(this.entry, this.objective.getHandle());
        if (triggerable) score.unlock(); else score.lock();
    }

    public net.kyori.adventure.text.Component customName() {
        Scoreboard board = this.objective.checkState().board;
        ReadOnlyScoreInfo info = board.getPlayerScoreInfo(this.entry, this.objective.getHandle());
        if (info == null) return null;
        net.minecraft.network.chat.Component display = board.getOrCreatePlayerScore(this.entry, this.objective.getHandle()).display();
        return display == null ? null : io.papermc.paper.adventure.PaperAdventure.asAdventure(display);
    }

    public void customName(net.kyori.adventure.text.Component customName) {
        this.objective.checkState().board.getOrCreatePlayerScore(this.entry, this.objective.getHandle())
                .display(customName == null ? null : io.papermc.paper.adventure.PaperAdventure.asVanilla(customName));
    }
}
