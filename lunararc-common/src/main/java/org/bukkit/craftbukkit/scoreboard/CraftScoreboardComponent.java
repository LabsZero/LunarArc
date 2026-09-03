package org.bukkit.craftbukkit.scoreboard;

abstract class CraftScoreboardComponent {
    private final CraftScoreboard scoreboard;

    CraftScoreboardComponent(CraftScoreboard scoreboard) {
        this.scoreboard = java.util.Objects.requireNonNull(scoreboard, "scoreboard");
    }

    abstract CraftScoreboard checkState();

    public CraftScoreboard getScoreboard() {
        return this.scoreboard;
    }

    abstract void unregister();
}
