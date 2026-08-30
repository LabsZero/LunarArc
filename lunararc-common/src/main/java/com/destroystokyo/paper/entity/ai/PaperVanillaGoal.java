package com.destroystokyo.paper.entity.ai;

import java.util.EnumSet;
import org.bukkit.entity.Mob;

/** Paper API view of a real loader-owned vanilla/mod NMS goal. */
public final class PaperVanillaGoal<T extends Mob> implements VanillaGoal<T> {
    private final net.minecraft.world.entity.ai.goal.Goal handle;
    private final GoalKey<T> key;
    private final EnumSet<GoalType> types;

    public PaperVanillaGoal(net.minecraft.world.entity.ai.goal.Goal handle) {
        this.handle = java.util.Objects.requireNonNull(handle, "handle");
        this.key = MobGoalHelper.getKey(handle.getClass());
        this.types = MobGoalHelper.vanillaToPaper(handle);
    }

    public net.minecraft.world.entity.ai.goal.Goal getHandle() { return handle; }
    @Override public boolean shouldActivate() { return handle.canUse(); }
    @Override public boolean shouldStayActive() { return handle.canContinueToUse(); }
    @Override public void start() { handle.start(); }
    @Override public void stop() { handle.stop(); }
    @Override public void tick() { handle.tick(); }
    @Override public GoalKey<T> getKey() { return key; }
    @Override public EnumSet<GoalType> getTypes() { return EnumSet.copyOf(types); }
}
