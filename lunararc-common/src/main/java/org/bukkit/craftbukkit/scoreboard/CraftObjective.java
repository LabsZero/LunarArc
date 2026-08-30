package org.bukkit.craftbukkit.scoreboard;

import com.google.common.base.Preconditions;
import net.minecraft.world.scores.Scoreboard;
import org.bukkit.OfflinePlayer;
import org.bukkit.craftbukkit.util.CraftChatMessage;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;
import org.bukkit.scoreboard.Score;

final class CraftObjective extends CraftScoreboardComponent implements Objective {
    private final net.minecraft.world.scores.Objective objective;
    private final CraftCriteria criteria;

    CraftObjective(CraftScoreboard scoreboard, net.minecraft.world.scores.Objective objective) {
        super(scoreboard);
        this.objective = java.util.Objects.requireNonNull(objective, "objective");
        this.criteria = CraftCriteria.getFromNMS(objective);
    }

    net.minecraft.world.scores.Objective getHandle() {
        return this.objective;
    }

    @Override
    public String getName() {
        checkState();
        return this.objective.getName();
    }

    @Override
    public String getDisplayName() {
        checkState();
        return CraftChatMessage.fromComponent(this.objective.getDisplayName());
    }

    @Override
    public void setDisplayName(String displayName) {
        Preconditions.checkArgument(displayName != null, "Display name cannot be null");
        checkState();
        this.objective.setDisplayName(CraftChatMessage.fromString(displayName)[0]);
    }

    // Paper Adventure objective API.
    public net.kyori.adventure.text.Component displayName() {
        checkState();
        return io.papermc.paper.adventure.PaperAdventure.asAdventure(this.objective.getDisplayName());
    }

    public void displayName(net.kyori.adventure.text.Component displayName) {
        checkState();
        this.objective.setDisplayName(io.papermc.paper.adventure.PaperAdventure.asVanilla(
                displayName == null ? net.kyori.adventure.text.Component.empty() : displayName));
    }

    @Override
    public String getCriteria() {
        checkState();
        return this.criteria.bukkitName;
    }

    @Override
    public Criteria getTrackedCriteria() {
        checkState();
        return this.criteria;
    }

    @Override
    public boolean isModifiable() {
        checkState();
        return !this.criteria.criteria.isReadOnly();
    }

    @Override
    public CraftScoreboard getScoreboard() {
        return super.getScoreboard();
    }

    @Override
    public void unregister() {
        CraftScoreboard scoreboard = checkState();
        scoreboard.board.removeObjective(this.objective);
    }

    @Override
    public void setDisplaySlot(DisplaySlot slot) {
        CraftScoreboard scoreboard = checkState();
        Scoreboard board = scoreboard.board;
        for (net.minecraft.world.scores.DisplaySlot nmsSlot : net.minecraft.world.scores.DisplaySlot.values()) {
            if (board.getDisplayObjective(nmsSlot) == this.objective) board.setDisplayObjective(nmsSlot, null);
        }
        if (slot != null) board.setDisplayObjective(CraftScoreboardTranslations.fromBukkitSlot(slot), this.objective);
    }

    @Override
    public DisplaySlot getDisplaySlot() {
        Scoreboard board = checkState().board;
        for (net.minecraft.world.scores.DisplaySlot nmsSlot : net.minecraft.world.scores.DisplaySlot.values()) {
            if (board.getDisplayObjective(nmsSlot) == this.objective) {
                return CraftScoreboardTranslations.toBukkitSlot(nmsSlot);
            }
        }
        return null;
    }

    @Override
    public void setRenderType(RenderType renderType) {
        Preconditions.checkArgument(renderType != null, "RenderType cannot be null");
        checkState();
        this.objective.setRenderType(CraftScoreboardTranslations.fromBukkitRender(renderType));
    }

    @Override
    public RenderType getRenderType() {
        checkState();
        return CraftScoreboardTranslations.toBukkitRender(this.objective.getRenderType());
    }

    @Override
    public Score getScore(OfflinePlayer player) {
        checkState();
        return new CraftScore(this, CraftScoreboard.getScoreHolder(player));
    }

    @Override
    public Score getScore(String entry) {
        Preconditions.checkArgument(entry != null, "Entry cannot be null");
        Preconditions.checkArgument(entry.length() <= Short.MAX_VALUE,
                "Score entry is longer than 32767 characters");
        checkState();
        return new CraftScore(this, CraftScoreboard.getScoreHolder(entry));
    }

    public Score getScoreFor(org.bukkit.entity.Entity entity) {
        Preconditions.checkArgument(entity instanceof org.bukkit.craftbukkit.entity.CraftEntity,
                "Entity is not owned by LunarArc");
        checkState();
        return new CraftScore(this, ((org.bukkit.craftbukkit.entity.CraftEntity) entity).getHandle());
    }

    public io.papermc.paper.scoreboard.numbers.NumberFormat numberFormat() {
        checkState();
        net.minecraft.network.chat.numbers.NumberFormat format = this.objective.numberFormat();
        return format == null ? null : io.papermc.paper.util.PaperScoreboardFormat.asPaper(format);
    }

    public void numberFormat(io.papermc.paper.scoreboard.numbers.NumberFormat format) {
        checkState();
        this.objective.setNumberFormat(format == null ? null : io.papermc.paper.util.PaperScoreboardFormat.asVanilla(format));
    }

    public boolean willAutoUpdateDisplay() {
        checkState();
        return this.objective.displayAutoUpdate();
    }

    public void setAutoUpdateDisplay(boolean autoUpdateDisplay) {
        checkState();
        this.objective.setDisplayAutoUpdate(autoUpdateDisplay);
    }

    @Override
    CraftScoreboard checkState() {
        CraftScoreboard scoreboard = super.getScoreboard();
        Preconditions.checkState(scoreboard.board.getObjective(this.objective.getName()) == this.objective,
                "Unregistered scoreboard component");
        return scoreboard;
    }

    @Override
    public int hashCode() {
        return this.objective.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CraftObjective that && this.objective.equals(that.objective);
    }
}
