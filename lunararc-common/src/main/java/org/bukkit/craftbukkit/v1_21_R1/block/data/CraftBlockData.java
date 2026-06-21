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

    /** Compat alias used by CraftServer.createBlockData(). */
    public static CraftBlockData create(BlockState state) {
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
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        String base = key != null ? key.toString() : "minecraft:air";
        // Build block-state properties string: "minecraft:stone_stairs[facing=north,...]"
        net.minecraft.world.level.block.state.StateDefinition<net.minecraft.world.level.block.Block,
                net.minecraft.world.level.block.state.BlockState> def = state.getBlock().getStateDefinition();
        if (def.getProperties().isEmpty()) return base;
        StringBuilder sb = new StringBuilder(base).append('[');
        boolean first = true;
        for (net.minecraft.world.level.block.state.properties.Property<?> prop : def.getProperties()) {
            if (!first) sb.append(',');
            first = false;
            sb.append(prop.getName()).append('=').append(getPropertyValueString(state, prop));
        }
        return sb.append(']').toString();
    }

    @SuppressWarnings("unchecked")
    private static <T extends Comparable<T>> String getPropertyValueString(
            net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.world.level.block.state.properties.Property<T> prop) {
        return prop.getName(state.getValue(prop));
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
    public float getDestroySpeed(@NotNull org.bukkit.inventory.ItemStack itemStack, boolean considerEnchants) {
        return 1.0f;
    }

    @Override
    public @NotNull org.bukkit.block.BlockState createBlockState() {
        return (org.bukkit.block.BlockState) java.lang.reflect.Proxy.newProxyInstance(
            org.bukkit.block.BlockState.class.getClassLoader(),
            new Class<?>[]{ org.bukkit.block.BlockState.class },
            (proxy, method, args) -> {
                if (method.getReturnType() == boolean.class) return false;
                if (method.getReturnType() == int.class) return 0;
                if (method.getReturnType() == float.class) return 0.0f;
                if (method.getReturnType() == org.bukkit.block.data.BlockData.class) return this;
                return null;
            });
    }

    @Override
    public void copyTo(@NotNull BlockData blockData) {}

    @Override
    public @NotNull org.bukkit.Material getPlacementMaterial() {
        return getMaterial();
    }

    @Override
    public @NotNull org.bukkit.util.VoxelShape getCollisionShape(@NotNull org.bukkit.Location location) {
        return (org.bukkit.util.VoxelShape) java.lang.reflect.Proxy.newProxyInstance(
            org.bukkit.util.VoxelShape.class.getClassLoader(),
            new Class<?>[]{ org.bukkit.util.VoxelShape.class },
            (proxy, method, args) -> {
                if (method.getReturnType() == boolean.class) return false;
                if (method.getReturnType() == java.util.Collection.class) return java.util.Collections.emptyList();
                return null;
            });
    }

    @Override
    public @NotNull org.bukkit.Color getMapColor() { return org.bukkit.Color.WHITE; }

    @Override
    public boolean isFaceSturdy(@NotNull org.bukkit.block.BlockFace face, @NotNull org.bukkit.block.BlockSupport support) { return false; }

    @Override
    public void mirror(@NotNull org.bukkit.block.structure.Mirror mirror) {}

    @Override
    public void rotate(@NotNull org.bukkit.block.structure.StructureRotation rotation) {}

    @Override
    public boolean isRandomlyTicked() {
        return state.isRandomlyTicking();
    }

    @Override
    public int getLightEmission() { return state.getLightEmission(); }

    @Override
    public @NotNull org.bukkit.block.PistonMoveReaction getPistonMoveReaction() {
        return org.bukkit.block.PistonMoveReaction.MOVE;
    }

    @Override
    public @NotNull org.bukkit.SoundGroup getSoundGroup() {
        net.minecraft.world.level.block.SoundType nms = state.getSoundType();
        return new org.bukkit.SoundGroup() {
            private org.bukkit.Sound toSound(net.minecraft.sounds.SoundEvent e) {
                try {
                    net.minecraft.resources.ResourceLocation rl =
                        BuiltInRegistries.SOUND_EVENT.getKey(e);
                    if (rl == null) return org.bukkit.Sound.BLOCK_STONE_BREAK;
                    return org.bukkit.Sound.valueOf(
                        rl.getPath().toUpperCase(java.util.Locale.ROOT).replace('.', '_'));
                } catch (Throwable t) { return org.bukkit.Sound.BLOCK_STONE_BREAK; }
            }
            @Override public float getVolume() { return nms.getVolume(); }
            @Override public float getPitch() { return nms.getPitch(); }
            @Override public @NotNull org.bukkit.Sound getBreakSound() { return toSound(nms.getBreakSound()); }
            @Override public @NotNull org.bukkit.Sound getStepSound() { return toSound(nms.getStepSound()); }
            @Override public @NotNull org.bukkit.Sound getPlaceSound() { return toSound(nms.getPlaceSound()); }
            @Override public @NotNull org.bukkit.Sound getHitSound() { return toSound(nms.getHitSound()); }
            @Override public @NotNull org.bukkit.Sound getFallSound() { return toSound(nms.getFallSound()); }
        };
    }

    @Override
    public boolean isOccluding() { return false; }

    @Override
    public boolean isPreferredTool(@NotNull org.bukkit.inventory.ItemStack tool) { return true; }

    @Override
    public boolean isSupported(@NotNull org.bukkit.Location location) { return true; }

    @Override
    public boolean isSupported(@NotNull org.bukkit.block.Block block) { return true; }

    @Override
    public boolean requiresCorrectToolForDrops() { return false; }

    @Override
    public @NotNull BlockData merge(@NotNull BlockData data) {
        return this;
    }

    @Override
    public @NotNull CraftBlockData clone() {
        return new CraftBlockData(state);
    }
}
