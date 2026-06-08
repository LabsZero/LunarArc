package org.bukkit.craftbukkit.v1_21_R1.block.data;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class CraftBlockData implements BlockData {
    private final BlockState state;

    public CraftBlockData(BlockState state) {
        this.state = state;
    }

    public BlockState getState() {
        return state;
    }

    public static CraftBlockData fromState(BlockState state) {
        return new CraftBlockData(state);
    }

    @Override
    public @NotNull Material getMaterial() {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (key == null) return Material.AIR;
        try {
            return Material.valueOf(key.getPath().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Material.AIR;
        }
    }

    @Override
    public @NotNull String getAsString() {
        return state.toString();
    }

    @Override
    public @NotNull String getAsString(boolean hideUnspecified) {
        return getAsString();
    }

    @Override
    public boolean matches(@Nullable BlockData data) {
        if (!(data instanceof CraftBlockData other)) return false;
        return this.state.getBlock() == other.state.getBlock();
    }

    @Override
    public @NotNull BlockData merge(@NotNull BlockData data) {
        return this;
    }

    @Override
    public @NotNull CraftBlockData clone() {
        return new CraftBlockData(state);
    }
}
