package org.bukkit.craftbukkit;

import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerTickRateManager;
import org.bukkit.ServerTickManager;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;


public final class CraftServerTickManager implements ServerTickManager {
    private final ServerTickRateManager handle;

    public CraftServerTickManager(MinecraftServer server) {
        this.handle = Objects.requireNonNull(server, "server").tickRateManager();
    }

    @Override public boolean isRunningNormally() { return handle.runsNormally(); }
    @Override public boolean isStepping() { return handle.isSteppingForward(); }
    @Override public boolean isSprinting() { return handle.isSprinting(); }
    @Override public boolean isFrozen() { return handle.isFrozen(); }
    @Override public float getTickRate() { return handle.tickrate(); }

    @Override
    public void setTickRate(float tick) {
        if (tick < 1.0F || tick > 10000.0F) {
            throw new IllegalArgumentException("Tick rate must be between 1.0 and 10000.0");
        }
        handle.setTickRate(tick);
    }

    @Override public void setFrozen(boolean frozen) { handle.setFrozen(frozen); }

    @Override
    public boolean stepGameIfFrozen(int ticks) {
        if (ticks <= 0) throw new IllegalArgumentException("Ticks must be greater than 0");
        return handle.stepGameIfPaused(ticks);
    }

    @Override public boolean stopStepping() { return handle.stopStepping(); }

    @Override
    public boolean requestGameToSprint(int ticks) {
        if (ticks <= 0) throw new IllegalArgumentException("Ticks must be greater than 0");
        return handle.requestGameToSprint(ticks);
    }

    @Override public boolean stopSprinting() { return handle.stopSprinting(); }

    @Override
    public boolean isFrozen(@NotNull Entity entity) {
        Objects.requireNonNull(entity, "entity");
        if (!(entity instanceof CraftEntity craft)) throw new IllegalArgumentException("Entity is not a CraftEntity");
        return handle.isEntityFrozen(craft.getHandle());
    }

    @Override public int getFrozenTicksToRun() { return handle.frozenTicksToRun(); }
}
