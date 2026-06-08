package org.bukkit.craftbukkit.v1_21_R1.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.Chunk;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.PistonMoveReaction;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_21_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_21_R1.block.data.CraftBlockData;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class CraftBlock implements Block {
    private final ServerLevel world;
    private final BlockPos position;

    public CraftBlock(ServerLevel world, BlockPos position) {
        this.world = world;
        this.position = position;
    }

    public static CraftBlock at(ServerLevel world, BlockPos position) {
        return new CraftBlock(world, position);
    }

    /** For backwards compat — existing callers use CraftBlock.create() */
    public static Block create(ServerLevel world, BlockPos position) {
        return new CraftBlock(world, position);
    }

    public static Block create(ServerLevel world, BlockPos position, net.minecraft.world.level.block.state.BlockState pendingState) {
        return new CraftBlock(world, position);
    }

    public ServerLevel getHandle() {
        return world;
    }

    public BlockPos getPosition() {
        return position;
    }

    // -----------------------------------------------------------------------
    // Block identity
    // -----------------------------------------------------------------------

    @Override
    public @NotNull World getWorld() {
        return new CraftWorld(world);
    }

    @Override
    public int getX() { return position.getX(); }

    @Override
    public int getY() { return position.getY(); }

    @Override
    public int getZ() { return position.getZ(); }

    @Override
    public @NotNull Location getLocation() {
        return new Location(new CraftWorld(world), position.getX(), position.getY(), position.getZ());
    }

    @Override
    public @Nullable Location getLocation(@Nullable Location loc) {
        if (loc != null) {
            loc.setWorld(new CraftWorld(world));
            loc.setX(position.getX());
            loc.setY(position.getY());
            loc.setZ(position.getZ());
        }
        return loc;
    }

    // -----------------------------------------------------------------------
    // Block type / data
    // -----------------------------------------------------------------------

    private net.minecraft.world.level.block.state.BlockState nmsState() {
        return world.getBlockState(position);
    }

    private static Material materialFromState(net.minecraft.world.level.block.state.BlockState state) {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (key == null) return Material.AIR;
        try {
            return Material.valueOf(key.getPath().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Material.AIR;
        }
    }

    @Override
    public @NotNull Material getType() {
        return materialFromState(nmsState());
    }

    @Override
    public void setType(@NotNull Material type) {
        setType(type, true);
    }

    @Override
    public void setType(@NotNull Material type, boolean applyPhysics) {
        String name = type.isLegacy() ? type.name().replace("LEGACY_", "") : type.name();
        ResourceLocation rl = ResourceLocation.withDefaultNamespace(name.toLowerCase(Locale.ROOT));
        net.minecraft.world.level.block.Block block = BuiltInRegistries.BLOCK.get(rl);
        if (block != null) {
            world.setBlock(position, block.defaultBlockState(), applyPhysics ? 3 : 2);
        }
    }

    @Override
    public @NotNull BlockData getBlockData() {
        return new CraftBlockData(nmsState());
    }

    @Override
    public void setBlockData(@NotNull BlockData data) {
        setBlockData(data, true);
    }

    @Override
    public void setBlockData(@NotNull BlockData data, boolean applyPhysics) {
        net.minecraft.world.level.block.state.BlockState nms;
        if (data instanceof CraftBlockData craftData) {
            nms = craftData.getState();
        } else {
            // Fallback: look up by material
            nms = BuiltInRegistries.BLOCK
                    .get(ResourceLocation.withDefaultNamespace(data.getMaterial().name().toLowerCase(Locale.ROOT)))
                    .defaultBlockState();
        }
        world.setBlock(position, nms, applyPhysics ? 3 : 2);
    }

    @Override
    public byte getData() { return 0; }

    @Override
    public void setData(byte data) {}

    @Override
    public void setData(byte data, boolean applyPhysics) {}

    // -----------------------------------------------------------------------
    // Block state
    // -----------------------------------------------------------------------

    @Override
    public @NotNull BlockState getState() {
        return getState(true);
    }

    @Override
    public @NotNull BlockState getState(boolean useSnapshot) {
        CraftBlock self = this;
        return (BlockState) java.lang.reflect.Proxy.newProxyInstance(
                BlockState.class.getClassLoader(),
                new Class<?>[]{ BlockState.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "getBlock"     -> self;
                    case "getType"      -> self.getType();
                    case "getWorld"     -> self.getWorld();
                    case "getX"         -> self.getX();
                    case "getY"         -> self.getY();
                    case "getZ"         -> self.getZ();
                    case "getLocation"  -> args != null && args.length > 0 ? self.getLocation((Location) args[0]) : self.getLocation();
                    case "getBlockData" -> self.getBlockData();
                    case "getRawData"   -> (byte) 0;
                    case "update"       -> {
                        boolean force = args != null && args.length > 0 && (boolean) args[0];
                        boolean physics = args == null || args.length < 2 || (boolean) args[1];
                        if (force) world.setBlock(position, nmsState(), physics ? 3 : 2);
                        yield true;
                    }
                    case "isPlaced"     -> true;
                    case "equals"       -> proxy == args[0];
                    case "hashCode"     -> System.identityHashCode(proxy);
                    default -> {
                        Class<?> rt = method.getReturnType();
                        yield rt == boolean.class ? false : rt == int.class ? 0 : null;
                    }
                });
    }

    // -----------------------------------------------------------------------
    // Block properties
    // -----------------------------------------------------------------------

    @Override
    public boolean isSolid() { return nmsState().isSolid(); }

    @Override
    public boolean isEmpty() { return nmsState().isAir(); }

    @Override
    public boolean isLiquid() { return !world.getFluidState(position).isEmpty(); }

    @Override
    public boolean isPassable() { return !nmsState().blocksMotion(); }

    @Override
    public boolean isPreferredTool(@NotNull ItemStack tool) { return true; }

    @Override
    public @NotNull PistonMoveReaction getPistonMoveReaction() {
        return PistonMoveReaction.MOVE;
    }

    @Override
    public boolean breakNaturally() { return breakNaturally(new ItemStack(Material.AIR)); }

    @Override
    public boolean breakNaturally(@NotNull ItemStack tool) {
        world.destroyBlock(position, true);
        return true;
    }

    @Override
    public boolean breakNaturally(@NotNull ItemStack tool, boolean triggerEffect) {
        return breakNaturally(tool);
    }

    @Override
    public boolean breakNaturally(@NotNull ItemStack tool, boolean triggerEffect, boolean dropExperience) {
        return breakNaturally(tool);
    }

    @Override
    public boolean applyBoneMeal(@NotNull BlockFace face) { return false; }

    @Override
    public @NotNull Collection<ItemStack> getDrops() { return Collections.emptyList(); }

    @Override
    public @NotNull Collection<ItemStack> getDrops(@NotNull ItemStack tool) { return Collections.emptyList(); }

    @Override
    public @NotNull Collection<ItemStack> getDrops(@NotNull ItemStack tool, @Nullable Entity entity) { return Collections.emptyList(); }

    @Override
    public float getDestroySpeed(@NotNull ItemStack itemStack, boolean considerEnchants) { return 1.0f; }

    @Override
    public boolean isPreferredTool(@NotNull ItemStack tool, @Nullable BlockFace face) { return true; }

    // -----------------------------------------------------------------------
    // Neighbours / faces
    // -----------------------------------------------------------------------

    @Override
    public @NotNull Block getRelative(int modX, int modY, int modZ) {
        return new CraftBlock(world, position.offset(modX, modY, modZ));
    }

    @Override
    public @NotNull Block getRelative(@NotNull BlockFace face) {
        return getRelative(face.getModX(), face.getModY(), face.getModZ());
    }

    @Override
    public @NotNull Block getRelative(@NotNull BlockFace face, int distance) {
        return getRelative(face.getModX() * distance, face.getModY() * distance, face.getModZ() * distance);
    }

    @Override
    public @NotNull BlockFace getFace(@NotNull Block block) {
        for (BlockFace face : BlockFace.values()) {
            Block rel = getRelative(face);
            if (rel.getX() == block.getX() && rel.getY() == block.getY() && rel.getZ() == block.getZ()) {
                return face;
            }
        }
        return BlockFace.SELF;
    }

    // -----------------------------------------------------------------------
    // Chunk / biome
    // -----------------------------------------------------------------------

    @Override
    public @NotNull Chunk getChunk() {
        return getWorld().getChunkAt(this);
    }

    @Override
    public @NotNull Biome getBiome() { return Biome.PLAINS; }

    @Override
    public void setBiome(@NotNull Biome bio) {}

    @Override
    public @NotNull Biome getComputedBiome() { return Biome.PLAINS; }

    // -----------------------------------------------------------------------
    // Light / temperature
    // -----------------------------------------------------------------------

    @Override
    public byte getLightLevel() { return (byte) world.getLightEmission(position); }

    @Override
    public byte getLightFromSky() { return 15; }

    @Override
    public byte getLightFromBlocks() { return (byte) world.getLightEmission(position); }

    @Override
    public double getTemperature() { return 0.5; }

    @Override
    public double getHumidity() { return 0.5; }

    // -----------------------------------------------------------------------
    // Metadata
    // -----------------------------------------------------------------------

    @Override
    public void setMetadata(@NotNull String key, @NotNull MetadataValue val) {}

    @Override
    public @NotNull List<MetadataValue> getMetadata(@NotNull String key) { return Collections.emptyList(); }

    @Override
    public boolean hasMetadata(@NotNull String key) { return false; }

    @Override
    public void removeMetadata(@NotNull String key, @NotNull Plugin plugin) {}

    // -----------------------------------------------------------------------
    // Object
    // -----------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CraftBlock other)) return false;
        return position.equals(other.position) && world == other.world;
    }

    @Override
    public int hashCode() {
        return position.hashCode() * 31 + System.identityHashCode(world);
    }

    @Override
    public String toString() {
        return "CraftBlock{world=" + world.dimension().location() + ",pos=" + position + ",type=" + getType() + "}";
    }

    // -----------------------------------------------------------------------
    // Raytrace stub
    // -----------------------------------------------------------------------

    @Override
    public @Nullable RayTraceResult rayTrace(@NotNull Location start, @NotNull Vector dir, double maxDist, @NotNull FluidCollisionMode mode) {
        return null;
    }

    @Override
    public @NotNull BoundingBox getBoundingBox() {
        return BoundingBox.of(getLocation(), getLocation().add(1, 1, 1));
    }
}
