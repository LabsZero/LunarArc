package org.bukkit.craftbukkit.scoreboard;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team.Visibility;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.craftbukkit.util.CraftChatMessage;
import org.bukkit.scoreboard.NameTagVisibility;
import org.bukkit.scoreboard.Team;

final class CraftTeam extends CraftScoreboardComponent implements Team {
    private final PlayerTeam team;

    CraftTeam(CraftScoreboard scoreboard, PlayerTeam team) {
        super(scoreboard);
        this.team = java.util.Objects.requireNonNull(team, "team");
    }

    @Override
    public String getName() {
        checkState();
        return this.team.getName();
    }

    @Override
    public String getDisplayName() {
        checkState();
        return CraftChatMessage.fromComponent(this.team.getDisplayName());
    }

    @Override
    public void setDisplayName(String displayName) {
        Preconditions.checkArgument(displayName != null, "Display name cannot be null");
        checkState();
        this.team.setDisplayName(CraftChatMessage.fromString(displayName)[0]);
    }

    @Override
    public String getPrefix() {
        checkState();
        return CraftChatMessage.fromComponent(this.team.getPlayerPrefix());
    }

    @Override
    public void setPrefix(String prefix) {
        Preconditions.checkArgument(prefix != null, "Prefix cannot be null");
        checkState();
        net.minecraft.network.chat.Component component = CraftChatMessage.fromStringOrNull(prefix);
        this.team.setPlayerPrefix(component == null ? net.minecraft.network.chat.Component.empty() : component);
    }

    @Override
    public String getSuffix() {
        checkState();
        return CraftChatMessage.fromComponent(this.team.getPlayerSuffix());
    }

    @Override
    public void setSuffix(String suffix) {
        Preconditions.checkArgument(suffix != null, "Suffix cannot be null");
        checkState();
        net.minecraft.network.chat.Component component = CraftChatMessage.fromStringOrNull(suffix);
        this.team.setPlayerSuffix(component == null ? net.minecraft.network.chat.Component.empty() : component);
    }

    @Override
    public ChatColor getColor() {
        checkState();
        return CraftChatMessage.getColor(this.team.getColor());
    }

    @Override
    public void setColor(ChatColor color) {
        Preconditions.checkArgument(color != null, "Color cannot be null");
        checkState();
        this.team.setColor(CraftChatMessage.getColor(color));
    }

    public net.kyori.adventure.text.Component displayName() {
        checkState();
        return io.papermc.paper.adventure.PaperAdventure.asAdventure(this.team.getDisplayName());
    }

    public void displayName(net.kyori.adventure.text.Component displayName) {
        checkState();
        this.team.setDisplayName(io.papermc.paper.adventure.PaperAdventure.asVanilla(
                displayName == null ? net.kyori.adventure.text.Component.empty() : displayName));
    }

    public net.kyori.adventure.text.Component prefix() {
        checkState();
        return io.papermc.paper.adventure.PaperAdventure.asAdventure(this.team.getPlayerPrefix());
    }

    public void prefix(net.kyori.adventure.text.Component prefix) {
        checkState();
        this.team.setPlayerPrefix(io.papermc.paper.adventure.PaperAdventure.asVanilla(
                prefix == null ? net.kyori.adventure.text.Component.empty() : prefix));
    }

    public net.kyori.adventure.text.Component suffix() {
        checkState();
        return io.papermc.paper.adventure.PaperAdventure.asAdventure(this.team.getPlayerSuffix());
    }

    public void suffix(net.kyori.adventure.text.Component suffix) {
        checkState();
        this.team.setPlayerSuffix(io.papermc.paper.adventure.PaperAdventure.asVanilla(
                suffix == null ? net.kyori.adventure.text.Component.empty() : suffix));
    }

    public boolean hasColor() {
        checkState();
        return this.team.getColor().getColor() != null;
    }

    public net.kyori.adventure.text.format.TextColor color() {
        checkState();
        net.kyori.adventure.text.format.TextColor color = io.papermc.paper.adventure.PaperAdventure.asAdventure(this.team.getColor());
        if (color == null) throw new IllegalStateException("Team does not have a named color");
        return color;
    }

    public void color(net.kyori.adventure.text.format.NamedTextColor color) {
        checkState();
        this.team.setColor(color == null ? net.minecraft.ChatFormatting.RESET : io.papermc.paper.adventure.PaperAdventure.asVanilla(color));
    }

    @Override
    public boolean allowFriendlyFire() {
        checkState();
        return this.team.isAllowFriendlyFire();
    }

    @Override
    public void setAllowFriendlyFire(boolean enabled) {
        checkState();
        this.team.setAllowFriendlyFire(enabled);
    }

    @Override
    public boolean canSeeFriendlyInvisibles() {
        checkState();
        return this.team.canSeeFriendlyInvisibles();
    }

    @Override
    public void setCanSeeFriendlyInvisibles(boolean enabled) {
        checkState();
        this.team.setSeeFriendlyInvisibles(enabled);
    }

    @Override
    public NameTagVisibility getNameTagVisibility() {
        checkState();
        return notchToBukkit(this.team.getNameTagVisibility());
    }

    @Override
    public void setNameTagVisibility(NameTagVisibility visibility) {
        Preconditions.checkArgument(visibility != null, "Visibility cannot be null");
        checkState();
        this.team.setNameTagVisibility(bukkitToNotch(visibility));
    }

    @Override
    public Set<OfflinePlayer> getPlayers() {
        checkState();
        ImmutableSet.Builder<OfflinePlayer> players = ImmutableSet.builder();
        for (String player : this.team.getPlayers()) players.add(Bukkit.getOfflinePlayer(player));
        return players.build();
    }

    @Override
    public Set<String> getEntries() {
        checkState();
        return ImmutableSet.copyOf(this.team.getPlayers());
    }

    @Override
    public int getSize() {
        checkState();
        return this.team.getPlayers().size();
    }

    @Override
    public void addPlayer(OfflinePlayer player) {
        Preconditions.checkArgument(player != null, "OfflinePlayer cannot be null");
        addEntry(player.getName());
    }

    @Override
    public void addEntry(String entry) {
        Preconditions.checkArgument(entry != null, "Entry cannot be null");
        checkState().board.addPlayerToTeam(entry, this.team);
    }

    public void addEntity(org.bukkit.entity.Entity entity) {
        Preconditions.checkArgument(entity instanceof org.bukkit.craftbukkit.entity.CraftEntity,
                "Entity is not owned by LunarArc");
        addEntry(((org.bukkit.craftbukkit.entity.CraftEntity) entity).getHandle().getScoreboardName());
    }

    public void addEntities(java.util.Collection<org.bukkit.entity.Entity> entities) {
        Preconditions.checkArgument(entities != null, "Entities cannot be null");
        addEntries(entities.stream().map(entity -> {
            Preconditions.checkArgument(entity instanceof org.bukkit.craftbukkit.entity.CraftEntity,
                    "Entity is not owned by LunarArc");
            return ((org.bukkit.craftbukkit.entity.CraftEntity) entity).getHandle().getScoreboardName();
        }).toList());
    }

    public void addEntries(java.util.Collection<String> entries) {
        Preconditions.checkArgument(entries != null, "Entries cannot be null");
        CraftScoreboard scoreboard = checkState();
        if (scoreboard.board instanceof net.minecraft.server.ServerScoreboard serverScoreboard) {
            for (String entry : entries) serverScoreboard.addPlayerToTeam(entry, this.team);
        } else {
            for (String entry : entries) scoreboard.board.addPlayerToTeam(entry, this.team);
        }
    }

    @Override
    public boolean removePlayer(OfflinePlayer player) {
        Preconditions.checkArgument(player != null, "OfflinePlayer cannot be null");
        return removeEntry(player.getName());
    }

    @Override
    public boolean removeEntry(String entry) {
        Preconditions.checkArgument(entry != null, "Entry cannot be null");
        CraftScoreboard scoreboard = checkState();
        if (!this.team.getPlayers().contains(entry)) return false;
        scoreboard.board.removePlayerFromTeam(entry, this.team);
        return true;
    }

    public boolean removeEntity(org.bukkit.entity.Entity entity) {
        Preconditions.checkArgument(entity instanceof org.bukkit.craftbukkit.entity.CraftEntity,
                "Entity is not owned by LunarArc");
        return removeEntry(((org.bukkit.craftbukkit.entity.CraftEntity) entity).getHandle().getScoreboardName());
    }

    public boolean removeEntries(java.util.Collection<String> entries) {
        Preconditions.checkArgument(entries != null, "Entries cannot be null");
        boolean changed = false;
        CraftScoreboard scoreboard = checkState();
        for (String entry : entries) {
            if (this.team.getPlayers().contains(entry)) {
                scoreboard.board.removePlayerFromTeam(entry, this.team);
                changed = true;
            }
        }
        return changed;
    }

    public boolean removeEntities(java.util.Collection<org.bukkit.entity.Entity> entities) {
        Preconditions.checkArgument(entities != null, "Entities cannot be null");
        return removeEntries(entities.stream().map(entity -> {
            Preconditions.checkArgument(entity instanceof org.bukkit.craftbukkit.entity.CraftEntity,
                    "Entity is not owned by LunarArc");
            return ((org.bukkit.craftbukkit.entity.CraftEntity) entity).getHandle().getScoreboardName();
        }).toList());
    }

    @Override
    public boolean hasPlayer(OfflinePlayer player) {
        Preconditions.checkArgument(player != null, "OfflinePlayer cannot be null");
        return hasEntry(player.getName());
    }

    @Override
    public boolean hasEntry(String entry) {
        Preconditions.checkArgument(entry != null, "Entry cannot be null");
        checkState();
        return this.team.getPlayers().contains(entry);
    }

    public boolean hasEntity(org.bukkit.entity.Entity entity) {
        Preconditions.checkArgument(entity instanceof org.bukkit.craftbukkit.entity.CraftEntity,
                "Entity is not owned by LunarArc");
        return hasEntry(((org.bukkit.craftbukkit.entity.CraftEntity) entity).getHandle().getScoreboardName());
    }

    @Override
    public void unregister() {
        CraftScoreboard scoreboard = checkState();
        scoreboard.board.removePlayerTeam(this.team);
    }

    @Override
    public OptionStatus getOption(Option option) {
        Preconditions.checkArgument(option != null, "Option cannot be null");
        checkState();
        return switch (option) {
            case NAME_TAG_VISIBILITY -> OptionStatus.values()[this.team.getNameTagVisibility().ordinal()];
            case DEATH_MESSAGE_VISIBILITY -> OptionStatus.values()[this.team.getDeathMessageVisibility().ordinal()];
            case COLLISION_RULE -> OptionStatus.values()[this.team.getCollisionRule().ordinal()];
        };
    }

    @Override
    public void setOption(Option option, OptionStatus status) {
        Preconditions.checkArgument(option != null, "Option cannot be null");
        Preconditions.checkArgument(status != null, "Option status cannot be null");
        checkState();
        switch (option) {
            case NAME_TAG_VISIBILITY -> this.team.setNameTagVisibility(Visibility.values()[status.ordinal()]);
            case DEATH_MESSAGE_VISIBILITY -> this.team.setDeathMessageVisibility(Visibility.values()[status.ordinal()]);
            case COLLISION_RULE -> this.team.setCollisionRule(net.minecraft.world.scores.Team.CollisionRule.values()[status.ordinal()]);
        }
    }

    public static Visibility bukkitToNotch(NameTagVisibility visibility) {
        return switch (visibility) {
            case ALWAYS -> Visibility.ALWAYS;
            case NEVER -> Visibility.NEVER;
            case HIDE_FOR_OTHER_TEAMS -> Visibility.HIDE_FOR_OTHER_TEAMS;
            case HIDE_FOR_OWN_TEAM -> Visibility.HIDE_FOR_OWN_TEAM;
        };
    }

    public static NameTagVisibility notchToBukkit(Visibility visibility) {
        return switch (visibility) {
            case ALWAYS -> NameTagVisibility.ALWAYS;
            case NEVER -> NameTagVisibility.NEVER;
            case HIDE_FOR_OTHER_TEAMS -> NameTagVisibility.HIDE_FOR_OTHER_TEAMS;
            case HIDE_FOR_OWN_TEAM -> NameTagVisibility.HIDE_FOR_OWN_TEAM;
        };
    }

    public @org.jetbrains.annotations.NotNull Iterable<? extends net.kyori.adventure.audience.Audience> audiences() {
        checkState();
        java.util.List<net.kyori.adventure.audience.Audience> result = new java.util.ArrayList<>();
        for (String playerName : this.team.getPlayers()) {
            org.bukkit.entity.Player player = Bukkit.getPlayerExact(playerName);
            if (player != null) result.add(player);
        }
        return result;
    }

    @Override
    CraftScoreboard checkState() {
        CraftScoreboard scoreboard = super.getScoreboard();
        Preconditions.checkState(scoreboard.board.getPlayerTeam(this.team.getName()) == this.team,
                "Unregistered scoreboard component");
        return scoreboard;
    }

    @Override
    public int hashCode() {
        return this.team.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CraftTeam that && this.team.equals(that.team);
    }
}
