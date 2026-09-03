package org.bukkit.craftbukkit.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.LevelData;
import org.bukkit.craftbukkit.block.CraftBlockEntityState;
import org.bukkit.craftbukkit.block.CraftBlockState;

/**
 * Virtual WorldGenLevel used by Bukkit tree-generation overloads.
 * Reads observe captured writes; writes never touch the real ServerLevel until replayed.
 */
public final class BlockStateListPopulator extends DummyGeneratorAccess {
    private final LevelAccessor world;
    private final LinkedHashMap<BlockPos, CapturedBlock> blocks = new LinkedHashMap<>();

    public BlockStateListPopulator(LevelAccessor world) {
        this.world = java.util.Objects.requireNonNull(world, "world");
    }

    @Override public BlockState getBlockState(BlockPos pos) {
        CapturedBlock captured = blocks.get(pos);
        return captured == null ? world.getBlockState(pos) : captured.state;
    }

    @Override public FluidState getFluidState(BlockPos pos) {
        return getBlockState(pos).getFluidState();
    }

    @Override public BlockEntity getBlockEntity(BlockPos pos) {
        CapturedBlock captured = blocks.get(pos);
        return captured == null ? world.getBlockEntity(pos) : captured.blockEntity;
    }

    @Override
    public boolean setBlock(BlockPos pos, BlockState state, int flags, int maxUpdateDepth) {
        BlockPos immutable = pos.immutable();
        BlockEntity blockEntity = null;
        if (state.getBlock() instanceof EntityBlock entityBlock) {
            BlockEntity current = getBlockEntity(immutable);
            if (current != null && current.isValidBlockState(state)) {
                current.setBlockState(state);
                blockEntity = current;
            } else {
                blockEntity = entityBlock.newBlockEntity(immutable, state);
            }
        }
        blocks.remove(immutable); // retain final write order like CraftBukkit/Paper
        blocks.put(immutable, new CapturedBlock(state, flags, blockEntity));
        return true;
    }

    @Override
    public boolean destroyBlock(BlockPos pos, boolean drop, Entity breakingEntity, int maxUpdateDepth) {
        BlockState state = getBlockState(pos);
        if (state.isAir()) return false;
        return setBlock(pos, state.getFluidState().createLegacyBlock(), net.minecraft.world.level.block.Block.UPDATE_ALL, maxUpdateDepth);
    }

    public List<CapturedState> getCapturedStates() {
        ServerLevel serverLevel = getMinecraftWorld();
        List<CapturedState> result = new ArrayList<>(blocks.size());
        for (Map.Entry<BlockPos, CapturedBlock> entry : blocks.entrySet()) {
            BlockPos pos = entry.getKey();
            CapturedBlock block = entry.getValue();
            CraftBlockState snapshot;
            if (block.blockEntity != null) {
                snapshot = new CraftBlockEntityState<>(serverLevel, block.blockEntity, true);
            } else {
                snapshot = new CraftBlockState(serverLevel, pos, block.state);
            }
            result.add(new CapturedState(snapshot, block.flags));
        }
        return result;
    }

    public void placeSomeBlocks(Predicate<? super org.bukkit.block.BlockState> filter,
            Consumer<? super org.bukkit.block.BlockState> beforePlace) {
        Predicate<? super org.bukkit.block.BlockState> actualFilter = filter == null ? state -> true : filter;
        Consumer<? super org.bukkit.block.BlockState> actualConsumer = beforePlace == null ? state -> {} : beforePlace;
        for (CapturedState captured : getCapturedStates()) {
            CraftBlockState state = captured.state;
            if (!actualFilter.test(state)) continue;
            actualConsumer.accept(state);
            state.update(true, (captured.flags & net.minecraft.world.level.block.Block.UPDATE_NEIGHBORS) != 0);
        }
    }

    public ServerLevel getMinecraftWorld() {
        if (world instanceof ServerLevel serverLevel) return serverLevel;
        if (world instanceof net.minecraft.server.level.WorldGenRegion region) return region.getLevel();
        throw new IllegalStateException("LevelAccessor is not backed by a ServerLevel: " + world.getClass().getName());
    }
    @Override public ServerLevel getLevel() { return getMinecraftWorld(); }
    @Override public int getMinBuildHeight() { return world.getMinBuildHeight(); }
    @Override public int getHeight() { return world.getHeight(); }
    @Override public boolean isStateAtPosition(BlockPos pos, Predicate<BlockState> predicate) { return predicate.test(getBlockState(pos)); }
    @Override public boolean isFluidAtPosition(BlockPos pos, Predicate<FluidState> predicate) { return predicate.test(getFluidState(pos)); }
    @Override public DimensionType dimensionType() { return world.dimensionType(); }
    @Override public RegistryAccess registryAccess() { return world.registryAccess(); }
    @Override public LevelData getLevelData() { return world.getLevelData(); }
    @Override public long nextSubTickCount() { return world.nextSubTickCount(); }
    @Override public RandomSource getRandom() { return world.getRandom(); }
    @Override public <T extends BlockEntity> Optional<T> getBlockEntity(BlockPos pos, BlockEntityType<T> type) {
        BlockEntity entity = getBlockEntity(pos);
        return entity != null && entity.getType() == type ? Optional.of((T) entity) : Optional.empty();
    }
    @Override public BlockPos getHeightmapPos(Heightmap.Types type, BlockPos pos) { return world.getHeightmapPos(type, pos); }
    @Override public int getHeight(Heightmap.Types type, int x, int z) { return world.getHeight(type, x, z); }
    @Override public int getRawBrightness(BlockPos pos, int ambientDarkness) { return world.getRawBrightness(pos, ambientDarkness); }
    @Override public int getBrightness(net.minecraft.world.level.LightLayer type, BlockPos pos) { return world.getBrightness(type, pos); }

    private record CapturedBlock(BlockState state, int flags, BlockEntity blockEntity) {}
    public record CapturedState(CraftBlockState state, int flags) {}
}
