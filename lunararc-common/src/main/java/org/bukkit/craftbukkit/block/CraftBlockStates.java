package org.bukkit.craftbukkit.block;

import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;

@SuppressWarnings({"unchecked", "rawtypes"})
public final class CraftBlockStates {

    private abstract static class BlockStateFactory<B extends CraftBlockState> {
        public final Class<B> blockStateType;

        BlockStateFactory(Class<B> blockStateType) {
            this.blockStateType = blockStateType;
        }

        public abstract B createBlockState(
            World world,
            BlockPos blockPosition,
            net.minecraft.world.level.block.state.BlockState blockData,
            BlockEntity tileEntity,
            boolean useSnapshot
        );
    }

    @FunctionalInterface
    private interface BlockEntityStateConstructor<T, B> {
        B create(World world, T blockEntity, boolean useSnapshot);
    }

    private static final class BlockEntityStateFactory<T extends BlockEntity, B extends CraftBlockEntityState<T>>
        extends BlockStateFactory<B> {

        private final BlockEntityStateConstructor<T, B> blockStateConstructor;
        private final BlockEntityType<? extends T> tileEntityConstructor;

        private BlockEntityStateFactory(
            Class<B> blockStateType,
            BlockEntityStateConstructor<T, B> blockStateConstructor,
            BlockEntityType<? extends T> tileEntityConstructor
        ) {
            super(blockStateType);
            this.blockStateConstructor = blockStateConstructor;
            this.tileEntityConstructor = tileEntityConstructor;
        }

        @Override
        public B createBlockState(
            World world,
            BlockPos blockPosition,
            net.minecraft.world.level.block.state.BlockState blockData,
            BlockEntity tileEntity,
            boolean useSnapshot
        ) {
            if (world != null) {
                Preconditions.checkState(
                    tileEntity != null,
                    "Tile is null, asynchronous access? %s",
                    CraftBlock.at(((CraftWorld) world).getHandle(), blockPosition)
                );
            } else if (tileEntity == null) {
                tileEntity = createTileEntity(blockPosition, blockData);
            }
            return createBlockState(world, (T) tileEntity, useSnapshot);
        }

        private T createTileEntity(
            BlockPos blockPosition,
            net.minecraft.world.level.block.state.BlockState blockData
        ) {
            return this.tileEntityConstructor.create(blockPosition, blockData);
        }

        private B createBlockState(World world, T tileEntity, boolean useSnapshot) {
            return this.blockStateConstructor.create(world, tileEntity, useSnapshot);
        }
    }

    private static final Map<Material, BlockStateFactory<?>> FACTORIES = new HashMap<>();
    private static final Map<BlockEntityType<?>, BlockStateFactory<?>> FACTORIES_BY_BLOCK_ENTITY_TYPE = new HashMap<>();

    /**
     * Hybrid fallback for loader/mod block entities that do not have a specialized
     * Bukkit state class. They still need a TileState-capable wrapper so placement
     * events can inspect/copy their loader-owned block entity without crashing.
     */
    private static final BlockStateFactory<?> GENERIC_BLOCK_ENTITY_FACTORY =
        new BlockStateFactory<CraftBlockEntityState>(CraftBlockEntityState.class) {
            @Override
            public CraftBlockEntityState createBlockState(
                World world,
                BlockPos blockPosition,
                net.minecraft.world.level.block.state.BlockState blockData,
                BlockEntity tileEntity,
                boolean useSnapshot
            ) {
                Preconditions.checkState(tileEntity != null, "Missing block entity for hybrid TileState at %s", blockPosition);
                return new CraftBlockEntityState(world, tileEntity, useSnapshot);
            }
        };

    private static final BlockStateFactory<?> DEFAULT_FACTORY = new BlockStateFactory<CraftBlockState>(CraftBlockState.class) {
        @Override
        public CraftBlockState createBlockState(
            World world,
            BlockPos blockPosition,
            net.minecraft.world.level.block.state.BlockState blockData,
            BlockEntity tileEntity,
            boolean useSnapshot
        ) {
            Preconditions.checkState(
                tileEntity == null,
                "Unexpected BlockState for %s",
                CraftMagicNumbers.getMaterial(blockData.getBlock())
            );
            return new CraftBlockState(world, blockPosition, blockData);
        }
    };

    static {
        // Paper 26.3 block-entity state mappings. The registered factories are also
        // keyed by BlockEntityType so modded block entities can fall back correctly.
        register(net.minecraft.world.level.block.entity.BlockEntityTypes.BANNER, CraftBanner.class, CraftBanner::new);
        register(net.minecraft.world.level.block.entity.BlockEntityTypes.BARREL, CraftBarrel.class, CraftBarrel::new);
        register(net.minecraft.world.level.block.entity.BlockEntityTypes.BEACON, CraftBeacon.class, CraftBeacon::new);
        register(net.minecraft.world.level.block.entity.BlockEntityTypes.BEEHIVE, CraftBeehive.class, CraftBeehive::new);
        register(net.minecraft.world.level.block.entity.BlockEntityTypes.BELL, CraftBell.class, CraftBell::new);
        register(net.minecraft.world.level.block.entity.BlockEntityTypes.BLAST_FURNACE, CraftBlastFurnace.class, CraftBlastFurnace::new);
        register(net.minecraft.world.level.block.entity.BlockEntityTypes.BREWING_STAND, CraftBrewingStand.class, CraftBrewingStand::new);
        register(net.minecraft.world.level.block.entity.BlockEntityTypes.BRUSHABLE_BLOCK, CraftBrushableBlock.class, CraftBrushableBlock::new);
        register(net.minecraft.world.level.block.entity.BlockEntityTypes.CALIBRATED_SCULK_SENSOR, CraftCalibratedSculkSensor.class, CraftCalibratedSculkSensor::new);
        register(net.minecraft.world.level.block.entity.BlockEntityTypes.CAMPFIRE, CraftCampfire.class, CraftCampfire::new);
        register(net.minecraft.world.level.block.entity.BlockEntityTypes.CHEST, CraftChest.class, CraftChest::new);
        register(net.minecraft.world.level.block.entity.BlockEntityTypes.CHISELED_BOOKSHELF, CraftChiseledBookshelf.class, CraftChiseledBookshelf::new);
        register(net.minecraft.world.level.block.entity.BlockEntityTypes.COMMAND_BLOCK, CraftCommandBlock.class, CraftCommandBlock::new);
        register(net.minecraft.world.level.block.entity.BlockEntityTypes.COMPARATOR, CraftComparator.class, CraftComparator::new);
        register(net.minecraft.world.level.block.entity.BlockEntityTypes.CONDUIT, CraftConduit.class, CraftConduit::new);
        register(net.minecraft.world.level.block.entity.BlockEntityTypes.COPPER_GOLEM_STATUE, CraftCopperGolemStatue.class, CraftCopperGolemStatue::new);
        register(net.minecraft.world.level.block.entity.BlockEntityTypes.CRAFTER, CraftCrafter.class, CraftCrafter::new);
        register(net.minecraft.world.level.block.entity.BlockEntityTypes.CREAKING_HEART, CraftCreakingHeart.class, CraftCreakingHeart::new);
        register(net.minecraft.world.level.block.entity.BlockEntityTypes.DAYLIGHT_DETECTOR, CraftDaylightDetector.class, CraftDaylightDetector::new);
        register(net.minecraft.world.level.block.entity.BlockEntityTypes.DECORATED_POT, CraftDecoratedPot.class, CraftDecoratedPot::new);
        register(net.minecraft.world.level.block.entity.BlockEntityTypes.DISPENSER, CraftDispenser.class, CraftDispenser::new);
        register(net.minecraft.world.level.block.entity.BlockEntityTypes.DROPPER, CraftDropper.class, CraftDropper::new);
        register(net.minecraft.world.level.block.entity.BlockEntityTypes.ENCHANTING_TABLE, CraftEnchantingTable.class, CraftEnchantingTable::new);
        register(net.minecraft.world.level.block.entity.BlockEntityTypes.END_GATEWAY, CraftEndGateway.class, CraftEndGateway::new);
        register(net.minecraft.world.level.block.entity.BlockEntityTypes.END_PORTAL, CraftEndPortal.class, CraftEndPortal::new);
        register(net.minecraft.world.level.block.entity.BlockEntityTypes.ENDER_CHEST, CraftEnderChest.class, CraftEnderChest::new);
        register(net.minecraft.world.level.block.entity.BlockEntityTypes.FURNACE, CraftFurnaceFurnace.class, CraftFurnaceFurnace::new);
        register(net.minecraft.world.level.block.entity.BlockEntityTypes.HANGING_SIGN, CraftHangingSign.class, CraftHangingSign::new);
        register(net.minecraft.world.level.block.entity.BlockEntityTypes.HOPPER, CraftHopper.class, CraftHopper::new);
        register(net.minecraft.world.level.block.entity.BlockEntityTypes.JIGSAW, CraftJigsaw.class, CraftJigsaw::new);
        register(net.minecraft.world.level.block.entity.BlockEntityTypes.JUKEBOX, CraftJukebox.class, CraftJukebox::new);
        register(net.minecraft.world.level.block.entity.BlockEntityTypes.LECTERN, CraftLectern.class, CraftLectern::new);
        register(net.minecraft.world.level.block.entity.BlockEntityTypes.MOB_SPAWNER, CraftCreatureSpawner.class, CraftCreatureSpawner::new);
        register(net.minecraft.world.level.block.entity.BlockEntityTypes.PISTON, CraftMovingPiston.class, CraftMovingPiston::new);
        register(net.minecraft.world.level.block.entity.BlockEntityTypes.POTENT_SULFUR, CraftPotentSulfur.class, CraftPotentSulfur::new);
        register(net.minecraft.world.level.block.entity.BlockEntityTypes.SCULK_CATALYST, CraftSculkCatalyst.class, CraftSculkCatalyst::new);
        register(net.minecraft.world.level.block.entity.BlockEntityTypes.SCULK_SENSOR, CraftSculkSensor.class, CraftSculkSensor::new);
        register(net.minecraft.world.level.block.entity.BlockEntityTypes.SCULK_SHRIEKER, CraftSculkShrieker.class, CraftSculkShrieker::new);
        register(net.minecraft.world.level.block.entity.BlockEntityTypes.SHELF, CraftShelf.class, CraftShelf::new);
        register(net.minecraft.world.level.block.entity.BlockEntityTypes.SHULKER_BOX, CraftShulkerBox.class, CraftShulkerBox::new);
        register(net.minecraft.world.level.block.entity.BlockEntityTypes.SIGN, CraftSign.class, CraftSign::new);
        register(net.minecraft.world.level.block.entity.BlockEntityTypes.SKULL, CraftSkull.class, CraftSkull::new);
        register(net.minecraft.world.level.block.entity.BlockEntityTypes.SMOKER, CraftSmoker.class, CraftSmoker::new);
        register(net.minecraft.world.level.block.entity.BlockEntityTypes.STRUCTURE_BLOCK, CraftStructureBlock.class, CraftStructureBlock::new);
        register(net.minecraft.world.level.block.entity.BlockEntityTypes.TEST_BLOCK, CraftTestBlock.class, CraftTestBlock::new);
        register(net.minecraft.world.level.block.entity.BlockEntityTypes.TEST_INSTANCE_BLOCK, CraftTestInstanceBlock.class, CraftTestInstanceBlock::new);
        register(net.minecraft.world.level.block.entity.BlockEntityTypes.TRAPPED_CHEST, CraftChest.class, CraftChest::new);
        register(net.minecraft.world.level.block.entity.BlockEntityTypes.TRIAL_SPAWNER, CraftTrialSpawner.class, CraftTrialSpawner::new);
        register(net.minecraft.world.level.block.entity.BlockEntityTypes.VAULT, CraftVault.class, CraftVault::new);
    }

    private static void register(Material blockType, BlockStateFactory<?> factory) {
        FACTORIES.put(blockType, factory);
    }

    private static void register(BlockEntityType<?> type, BlockStateFactory<?> factory) {
        FACTORIES_BY_BLOCK_ENTITY_TYPE.put(type, factory);
    }

    private static <T extends BlockEntity, B extends CraftBlockEntityState<T>> void register(
        BlockEntityType<? extends T> blockEntityType,
        Class<B> blockStateType,
        BlockEntityStateConstructor<T, B> blockStateConstructor
    ) {
        BlockStateFactory<B> factory = new BlockEntityStateFactory<>(blockStateType, blockStateConstructor, blockEntityType);

        // Do not access BlockEntityType.validBlocks directly. Paper widens that field,
        // but loader-owned Minecraft intentionally keeps it encapsulated. The vanilla
        // public isValid(BlockState) method expresses the same membership check without
        // relying on a Mixin-added interface or Paper's widened NMS ABI.
        for (net.minecraft.world.level.block.Block block : net.minecraft.core.registries.BuiltInRegistries.BLOCK) {
            if (blockEntityType.isValid(block.defaultBlockState())) {
                Material material = CraftMagicNumbers.getMaterial(block);
                if (material != null) register(material, factory);
            }
        }
        register(blockEntityType, factory);
    }

    private static BlockStateFactory<?> getFactory(Material material) {
        return FACTORIES.getOrDefault(material, DEFAULT_FACTORY);
    }

    private static BlockStateFactory<?> getFactory(Material material, BlockEntityType<?> type) {
        if (type == null) return getFactory(material);

        BlockStateFactory<?> typed = FACTORIES_BY_BLOCK_ENTITY_TYPE.get(type);
        if (typed != null) return typed;

        // A loader/mod block entity has no vanilla Craft* specialization. Returning a
        // generic CraftBlockEntityState keeps the Bukkit/Paper event path functional
        // while preserving the real loader-owned BlockEntity instance and snapshot.
        return GENERIC_BLOCK_ENTITY_FACTORY;
    }

    public static Class<? extends CraftBlockState> getBlockStateType(Material material) {
        Preconditions.checkNotNull(material, "material is null");
        return getFactory(material).blockStateType;
    }

    public static BlockEntity createNewBlockEntity(Material material) {
        BlockStateFactory<?> factory = getFactory(material);
        if (factory instanceof BlockEntityStateFactory) {
            return ((BlockEntityStateFactory<?, ?>) factory).createTileEntity(
                BlockPos.ZERO,
                CraftMagicNumbers.getBlock(material).defaultBlockState()
            );
        }
        return null;
    }

    /** @deprecated pre-26 name, use {@link #createNewBlockEntity(Material)}. */
    @Deprecated
    public static BlockEntity createNewTileEntity(Material material) {
        return createNewBlockEntity(material);
    }

    public static Class<? extends CraftBlockState> getBlockStateType(BlockEntityType<?> blockEntityType) {
        Preconditions.checkNotNull(blockEntityType, "blockEntityType is null");
        return getFactory(null, blockEntityType).blockStateType;
    }

    public static BlockState getBlockState(Block block) {
        return getBlockState(block, true);
    }

    public static BlockState getBlockState(Block block, boolean useSnapshot) {
        Preconditions.checkNotNull(block, "block is null");
        CraftBlock craftBlock = (CraftBlock) block;
        CraftWorld world = (CraftWorld) block.getWorld();
        BlockPos blockPosition = craftBlock.getPosition();
        net.minecraft.world.level.block.state.BlockState blockData = craftBlock.getHandle().getBlockState(blockPosition);
        BlockEntity tileEntity = craftBlock.getHandle().getBlockEntity(blockPosition);

        CraftBlockState blockState = getBlockState(world, blockPosition, blockData, tileEntity, useSnapshot);
        blockState.setWorldHandle(craftBlock.getHandle());
        return blockState;
    }

    @Deprecated
    public static BlockState getBlockState(BlockPos blockPosition, Material material, @Nullable CompoundTag blockEntityTag) {
        return getBlockState(io.lunararcdevs.lunararc.common.LunarArcServerAccess.getMinecraftServer().registryAccess(), blockPosition, material, blockEntityTag);
    }

    public static BlockState getBlockState(LevelReader world, BlockPos blockPosition, Material material, @Nullable CompoundTag blockEntityTag) {
        return getBlockState(world.registryAccess(), blockPosition, material, blockEntityTag);
    }

    public static BlockState getBlockState(RegistryAccess registry, BlockPos blockPosition, Material material, @Nullable CompoundTag blockEntityTag) {
        Preconditions.checkNotNull(material, "material is null");
        net.minecraft.world.level.block.state.BlockState blockData = CraftMagicNumbers.getBlock(material).defaultBlockState();
        return getBlockState(registry, blockPosition, blockData, blockEntityTag);
    }

    @Deprecated
    public static BlockState getBlockState(
        net.minecraft.world.level.block.state.BlockState blockData,
        @Nullable CompoundTag blockEntityTag
    ) {
        return getBlockState(io.lunararcdevs.lunararc.common.LunarArcServerAccess.getMinecraftServer().registryAccess(), BlockPos.ZERO, blockData, blockEntityTag);
    }

    public static BlockState getBlockState(
        LevelReader world,
        BlockPos blockPosition,
        net.minecraft.world.level.block.state.BlockState blockData,
        @Nullable CompoundTag blockEntityTag
    ) {
        return getBlockState(world.registryAccess(), blockPosition, blockData, blockEntityTag);
    }

    public static BlockState getBlockState(
        RegistryAccess registry,
        BlockPos blockPosition,
        net.minecraft.world.level.block.state.BlockState blockData,
        @Nullable CompoundTag blockEntityTag
    ) {
        Preconditions.checkNotNull(blockPosition, "blockPosition is null");
        Preconditions.checkNotNull(blockData, "blockData is null");
        BlockEntity tileEntity = blockEntityTag == null
            ? null
            : BlockEntity.loadStatic(blockPosition, blockData, blockEntityTag, registry);
        return getBlockState(null, blockPosition, blockData, tileEntity);
    }

    public static CraftBlockState getBlockState(
        World world,
        BlockPos blockPosition,
        net.minecraft.world.level.block.state.BlockState blockData,
        BlockEntity tileEntity
    ) {
        return getBlockState(world, blockPosition, blockData, tileEntity, true);
    }

    public static CraftBlockState getBlockState(
        World world,
        BlockPos blockPosition,
        net.minecraft.world.level.block.state.BlockState blockData,
        BlockEntity tileEntity,
        boolean useSnapshot
    ) {
        Material material = CraftMagicNumbers.getMaterial(blockData.getBlock());
        BlockStateFactory<?> factory;

        if (world != null && tileEntity == null && isBlockEntityOptional(material)) {
            factory = DEFAULT_FACTORY;
        } else {
            factory = getFactory(material, tileEntity != null ? tileEntity.getType() : null);
        }
        return factory.createBlockState(world, blockPosition, blockData, tileEntity, useSnapshot);
    }

    public static boolean isBlockEntityOptional(Material material) {
        return material == Material.MOVING_PISTON;
    }

    /** @deprecated pre-26 name, use {@link #isBlockEntityOptional(Material)}. */
    @Deprecated
    public static boolean isTileEntityOptional(Material material) {
        return isBlockEntityOptional(material);
    }

    public static CraftBlockState getBlockState(LevelAccessor world, BlockPos pos) {
        if (world instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            return new CraftBlockState(CraftBlock.at(serverLevel, pos));
        }
        return CraftBlockState.unplacedAt(pos, world.getBlockState(pos));
    }

    public static CraftBlockState getBlockState(LevelAccessor world, BlockPos pos, int flag) {
        if (world instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            return new CraftBlockState(CraftBlock.at(serverLevel, pos), flag);
        }
        CraftBlockState state = CraftBlockState.unplacedAt(pos, world.getBlockState(pos));
        state.flag = flag;
        state.setWorldHandle(world);
        return state;
    }

    @Nullable
    public static BlockEntityType<?> getBlockEntityType(Material material) {
        BlockStateFactory<?> factory = FACTORIES.get(material);
        return factory instanceof BlockEntityStateFactory<?, ?> blockEntityStateFactory
            ? blockEntityStateFactory.tileEntityConstructor
            : null;
    }

    private CraftBlockStates() {
    }
}
