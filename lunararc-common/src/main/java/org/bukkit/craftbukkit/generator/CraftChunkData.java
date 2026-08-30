package org.bukkit.craftbukkit.generator;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import org.bukkit.craftbukkit.legacy.CraftLegacy;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.material.MaterialData;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Concrete 1.21.1 ChunkData implementation. ChunkData is intentionally detached
 * generator state, but every stored block is a real NMS BlockState rather than
 * a fabricated Bukkit value.
 */
@SuppressWarnings("deprecation")
public final class CraftChunkData implements ChunkGenerator.ChunkData {
    private final int minHeight;
    private final int maxHeight;
    private final Map<Long, BlockState> blocks = new HashMap<>();
    private final WeakReference<net.minecraft.world.level.chunk.ChunkAccess> weakChunk;

    public CraftChunkData(int minHeight, int maxHeight) {
        this(minHeight, maxHeight, null);
    }

    public CraftChunkData(int minHeight, int maxHeight, net.minecraft.world.level.chunk.ChunkAccess chunk) {
        if (maxHeight <= minHeight) throw new IllegalArgumentException("maxHeight must be greater than minHeight");
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        this.weakChunk = new WeakReference<>(chunk);
    }

    @Override public int getMinHeight() { return this.minHeight; }
    @Override public int getMaxHeight() { return this.maxHeight; }

    @Override
    public @NotNull Biome getBiome(int x, int y, int z) {
        net.minecraft.world.level.chunk.ChunkAccess chunk = this.weakChunk.get();
        if (chunk == null) {
            throw new IllegalStateException("ChunkData is no longer linked to a generation chunk");
        }
        return org.bukkit.craftbukkit.block.CraftBiome.minecraftHolderToBukkit(
                chunk.getNoiseBiome(x >> 2, y >> 2, z >> 2));
    }

    /** Release the generation-chunk view once the plugin callback has completed. */
    public void breakLink() { this.weakChunk.clear(); }

    @Override
    public void setBlock(int x, int y, int z, @NotNull Material material) {
        Objects.requireNonNull(material, "material");
        BlockState state = material.isLegacy()
                ? CraftLegacy.fromLegacyData(material, (byte) 0)
                : CraftMagicNumbers.getBlock(material).defaultBlockState();
        this.setBlockState(x, y, z, state);
    }

    @Override
    public void setBlock(int x, int y, int z, @NotNull MaterialData material) {
        Objects.requireNonNull(material, "material");
        Material type = material.getItemType();
        BlockState state = type.isLegacy()
                ? CraftLegacy.fromLegacyData(type, material.getData())
                : CraftMagicNumbers.getBlock(type).defaultBlockState();
        this.setBlockState(x, y, z, state);
    }

    @Override
    public void setBlock(int x, int y, int z, @NotNull BlockData blockData) {
        Objects.requireNonNull(blockData, "blockData");
        if (!(blockData instanceof CraftBlockData craft)) {
            throw new IllegalArgumentException("BlockData must be a LunarArc/CraftBlockData instance");
        }
        this.setBlockState(x, y, z, craft.getState());
    }

    @Override
    public void setRegion(int xMin, int yMin, int zMin, int xMax, int yMax, int zMax, @NotNull Material material) {
        Objects.requireNonNull(material, "material");
        BlockState state = material.isLegacy()
                ? CraftLegacy.fromLegacyData(material, (byte) 0)
                : CraftMagicNumbers.getBlock(material).defaultBlockState();
        this.setRegionState(xMin, yMin, zMin, xMax, yMax, zMax, state);
    }

    @Override
    public void setRegion(int xMin, int yMin, int zMin, int xMax, int yMax, int zMax, @NotNull MaterialData material) {
        Objects.requireNonNull(material, "material");
        Material type = material.getItemType();
        BlockState state = type.isLegacy()
                ? CraftLegacy.fromLegacyData(type, material.getData())
                : CraftMagicNumbers.getBlock(type).defaultBlockState();
        this.setRegionState(xMin, yMin, zMin, xMax, yMax, zMax, state);
    }

    @Override
    public void setRegion(int xMin, int yMin, int zMin, int xMax, int yMax, int zMax, @NotNull BlockData blockData) {
        Objects.requireNonNull(blockData, "blockData");
        if (!(blockData instanceof CraftBlockData craft)) {
            throw new IllegalArgumentException("BlockData must be a LunarArc/CraftBlockData instance");
        }
        this.setRegionState(xMin, yMin, zMin, xMax, yMax, zMax, craft.getState());
    }

    @Override
    public @NotNull Material getType(int x, int y, int z) {
        BlockState state = this.getBlockState(x, y, z);
        if (state.isAir()) return Material.AIR;
        Material material = CraftMagicNumbers.getMaterial(state.getBlock());
        if (material == null) {
            throw new IllegalStateException("No Bukkit Material is registered for NMS block "
                    + net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()));
        }
        return material;
    }

    @Override
    public @NotNull MaterialData getTypeAndData(int x, int y, int z) {
        return CraftLegacy.toLegacy(this.getBlockState(x, y, z));
    }

    @Override
    public @NotNull BlockData getBlockData(int x, int y, int z) {
        return CraftBlockData.fromData(this.getBlockState(x, y, z));
    }

    @Override
    public byte getData(int x, int y, int z) {
        return CraftLegacy.toLegacyData(this.getBlockState(x, y, z));
    }

    public BlockState getBlockState(int x, int y, int z) {
        if (!inBounds(x, y, z)) return Blocks.AIR.defaultBlockState();
        return this.blocks.getOrDefault(key(x, y, z), Blocks.AIR.defaultBlockState());
    }

    /** Apply this detached Bukkit generation result into a real NMS chunk. */
    public void applyTo(net.minecraft.world.level.chunk.ChunkAccess chunk) {
        int baseX = chunk.getPos().getMinBlockX();
        int baseZ = chunk.getPos().getMinBlockZ();
        for (Map.Entry<Long, BlockState> entry : this.blocks.entrySet()) {
            long packed = entry.getKey();
            int x = (int) ((packed >>> 60) & 15L);
            int z = (int) ((packed >>> 56) & 15L);
            int y = (int) packed;
            BlockState state = entry.getValue();
            net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(baseX + x, y, baseZ + z);
            chunk.setBlockState(pos, state, false);
            if (state.hasBlockEntity() && state.getBlock() instanceof net.minecraft.world.level.block.EntityBlock entityBlock) {
                net.minecraft.world.level.block.entity.BlockEntity blockEntity = entityBlock.newBlockEntity(pos, state);
                if (blockEntity != null) chunk.setBlockEntity(blockEntity);
            }
        }
    }

    private void setBlockState(int x, int y, int z, BlockState state) {
        if (!inBounds(x, y, z)) return;
        long key = key(x, y, z);
        if (state.isAir()) this.blocks.remove(key);
        else this.blocks.put(key, state);
    }

    private void setRegionState(int xMin, int yMin, int zMin, int xMax, int yMax, int zMax, BlockState state) {
        xMin = Math.max(0, xMin);
        yMin = Math.max(this.minHeight, yMin);
        zMin = Math.max(0, zMin);
        xMax = Math.min(16, xMax);
        yMax = Math.min(this.maxHeight, yMax);
        zMax = Math.min(16, zMax);
        if (xMin >= xMax || yMin >= yMax || zMin >= zMax) return;
        for (int y = yMin; y < yMax; y++) {
            for (int x = xMin; x < xMax; x++) {
                for (int z = zMin; z < zMax; z++) this.setBlockState(x, y, z, state);
            }
        }
    }

    private boolean inBounds(int x, int y, int z) {
        return x >= 0 && x < 16 && z >= 0 && z < 16 && y >= this.minHeight && y < this.maxHeight;
    }

    private static long key(int x, int y, int z) {
        return ((long) (x & 15) << 60) | ((long) (z & 15) << 56) | (Integer.toUnsignedLong(y));
    }
}
