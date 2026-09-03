package org.bukkit.craftbukkit.advancement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Objects;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.server.PlayerAdvancements;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public final class CraftAdvancementProgress implements AdvancementProgress {
    private final Advancement advancement;
    private final AdvancementHolder holder;
    private final PlayerAdvancements playerData;
    private final net.minecraft.advancements.AdvancementProgress handle;

    public CraftAdvancementProgress(Advancement advancement, AdvancementHolder holder,
            PlayerAdvancements playerData, net.minecraft.advancements.AdvancementProgress handle) {
        this.advancement = Objects.requireNonNull(advancement, "advancement");
        this.holder = Objects.requireNonNull(holder, "holder");
        this.playerData = Objects.requireNonNull(playerData, "playerData");
        this.handle = Objects.requireNonNull(handle, "handle");
    }

    @Override
    public @NotNull Advancement getAdvancement() {
        return this.advancement;
    }

    @Override
    public boolean isDone() {
        return this.handle.isDone();
    }

    @Override
    public boolean awardCriteria(@NotNull String criteria) {
        Objects.requireNonNull(criteria, "criteria");
        return this.playerData.award(this.holder, criteria);
    }

    @Override
    public boolean revokeCriteria(@NotNull String criteria) {
        Objects.requireNonNull(criteria, "criteria");
        return this.playerData.revoke(this.holder, criteria);
    }

    @Override
    public @Nullable Date getDateAwarded(@NotNull String criteria) {
        Objects.requireNonNull(criteria, "criteria");
        CriterionProgress progress = this.handle.getCriterion(criteria);
        if (progress == null || progress.getObtained() == null) return null;
        return Date.from(progress.getObtained());
    }

    @Override
    public @NotNull Collection<String> getRemainingCriteria() {
        ArrayList<String> result = new ArrayList<>();
        this.handle.getRemainingCriteria().forEach(result::add);
        return Collections.unmodifiableList(result);
    }

    @Override
    public @NotNull Collection<String> getAwardedCriteria() {
        ArrayList<String> result = new ArrayList<>();
        this.handle.getCompletedCriteria().forEach(result::add);
        return Collections.unmodifiableList(result);
    }
}
