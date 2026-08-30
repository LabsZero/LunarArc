package org.bukkit.craftbukkit;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import net.minecraft.core.BlockPos;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.jetbrains.annotations.Nullable;


public final class CraftWorldBorder implements WorldBorder {
    private final @Nullable CraftWorld world;
    private final net.minecraft.world.level.border.WorldBorder handle;

    public CraftWorldBorder(CraftWorld world) {
        this.world = Objects.requireNonNull(world, "world");
        this.handle = world.getHandle().getWorldBorder();
    }

    public CraftWorldBorder(net.minecraft.world.level.border.WorldBorder handle) {
        this.world = null;
        this.handle = Objects.requireNonNull(handle, "handle");
    }

    @Override
    public @Nullable World getWorld() {
        return world;
    }

    @Override
    public void reset() {
        handle.applySettings(net.minecraft.world.level.border.WorldBorder.DEFAULT_SETTINGS);
    }

    @Override
    public double getSize() {
        return handle.getSize();
    }

    @Override
    public void setSize(double newSize) {
        setSize(newSize, TimeUnit.MILLISECONDS, 0L);
    }

    @Override
    public void setSize(double newSize, long seconds) {
        setSize(newSize, TimeUnit.SECONDS, seconds);
    }

    @Override
    public void setSize(double newSize, TimeUnit unit, long time) {
        Objects.requireNonNull(unit, "unit");
        if (time < 0L) throw new IllegalArgumentException("time cannot be negative");
        if (!Double.isFinite(newSize) || newSize < 1.0D || newSize > getMaxSize()) {
            throw new IllegalArgumentException("newSize must be between 1.0 and " + getMaxSize());
        }
        long millis;
        try {
            millis = Math.max(0L, unit.toMillis(time));
        } catch (ArithmeticException overflow) {
            millis = Long.MAX_VALUE;
        }
        if (millis == 0L) handle.setSize(newSize);
        else handle.lerpSizeBetween(handle.getSize(), newSize, millis);
    }

    @Override
    public Location getCenter() {
        return new Location(world, handle.getCenterX(), 0.0D, handle.getCenterZ());
    }

    @Override
    public void setCenter(double x, double z) {
        if (!Double.isFinite(x) || !Double.isFinite(z)) throw new IllegalArgumentException("border center must be finite");
        if (Math.abs(x) > getMaxCenterCoordinate() || Math.abs(z) > getMaxCenterCoordinate()) {
            throw new IllegalArgumentException("border center is outside the supported coordinate range");
        }
        handle.setCenter(x, z);
    }

    @Override
    public void setCenter(Location location) {
        Objects.requireNonNull(location, "location");
        if (world != null && location.getWorld() != null && location.getWorld() != world) {
            throw new IllegalArgumentException("location belongs to another world");
        }
        setCenter(location.getX(), location.getZ());
    }

    @Override
    public double getDamageBuffer() { return handle.getDamageSafeZone(); }

    @Override
    public void setDamageBuffer(double blocks) {
        if (blocks < 0.0D || !Double.isFinite(blocks)) throw new IllegalArgumentException("blocks must be finite and non-negative");
        handle.setDamageSafeZone(blocks);
    }

    @Override
    public double getDamageAmount() { return handle.getDamagePerBlock(); }

    @Override
    public void setDamageAmount(double damage) {
        if (damage < 0.0D || !Double.isFinite(damage)) throw new IllegalArgumentException("damage must be finite and non-negative");
        handle.setDamagePerBlock(damage);
    }

    @Override
    public int getWarningTime() { return handle.getWarningTime(); }

    @Override
    public void setWarningTime(int seconds) {
        if (seconds < 0) throw new IllegalArgumentException("warning time cannot be negative");
        handle.setWarningTime(seconds);
    }

    @Override
    public int getWarningDistance() { return handle.getWarningBlocks(); }

    @Override
    public void setWarningDistance(int distance) {
        if (distance < 0) throw new IllegalArgumentException("warning distance cannot be negative");
        handle.setWarningBlocks(distance);
    }

    @Override
    public boolean isInside(Location location) {
        Objects.requireNonNull(location, "location");
        if (world != null && location.getWorld() != world) return false;
        return handle.isWithinBounds(BlockPos.containing(location.getX(), location.getY(), location.getZ()));
    }

    @Override
    public double getMaxSize() {
        return net.minecraft.world.level.border.WorldBorder.MAX_SIZE;
    }

    @Override
    public double getMaxCenterCoordinate() {
        return net.minecraft.world.level.border.WorldBorder.MAX_CENTER_COORDINATE;
    }

    public net.minecraft.world.level.border.WorldBorder getHandle() {
        return handle;
    }

    public boolean isVirtual() {
        return world == null;
    }
}
