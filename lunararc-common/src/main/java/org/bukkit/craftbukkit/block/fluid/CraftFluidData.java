package org.bukkit.craftbukkit.block.fluid;

import io.papermc.paper.block.fluid.type.FlowingFluidData;
import java.util.Locale;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.FluidState;
import org.bukkit.Fluid;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;


public final class CraftFluidData implements FlowingFluidData {
    private final FluidState state;

    public CraftFluidData(FluidState state) {
        this.state = Objects.requireNonNull(state, "state");
    }

    public FluidState getHandle() { return state; }

    @Override
    public @NotNull Fluid getFluidType() {
        ResourceLocation id = BuiltInRegistries.FLUID.getKey(state.getType());
        if (id == null || !"minecraft".equals(id.getNamespace())) return Fluid.EMPTY;
        try { return Fluid.valueOf(id.getPath().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ignored) { return Fluid.EMPTY; }
    }

    @Override
    public @NotNull CraftFluidData clone() { return new CraftFluidData(state); }

    private static CraftWorld world(Location location) {
        Objects.requireNonNull(location, "location");
        if (!(location.getWorld() instanceof CraftWorld world)) {
            throw new IllegalArgumentException("Location must belong to a CraftWorld");
        }
        return world;
    }

    @Override
    public @NotNull Vector computeFlowDirection(@NotNull Location location) {
        CraftWorld world = world(location);
        BlockPos pos = new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        net.minecraft.world.phys.Vec3 flow = state.getFlow(world.getHandle(), pos);
        return new Vector(flow.x, flow.y, flow.z);
    }

    @Override public int getLevel() { return Math.max(0, Math.min(8, state.getAmount())); }

    @Override
    public float computeHeight(@NotNull Location location) {
        CraftWorld world = world(location);
        return state.getHeight(world.getHandle(), new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ()));
    }

    @Override public boolean isSource() { return state.isSource(); }

    @Override
    public boolean isFalling() {
        try {
            if (state.hasProperty(net.minecraft.world.level.material.FlowingFluid.FALLING)) {
                return state.getValue(net.minecraft.world.level.material.FlowingFluid.FALLING);
            }
        } catch (Throwable ignored) {}
        return false;
    }

    @Override public int hashCode() { return state.hashCode(); }
    @Override public boolean equals(Object o) { return o instanceof CraftFluidData other && state.equals(other.state); }
    @Override public String toString() { return "CraftFluidData{" + state + '}'; }
}
