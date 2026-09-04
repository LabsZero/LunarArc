package io.ampznetwork.lunararc.common.server;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A from-scratch, mixin-compatible engine for real Paper's anti-xray "HIDE" mode - verified against
 * Paper's own {@code patches/server/0992-Anti-Xray.patch} for the mechanism, config keys, and the
 * exact reveal-trigger algorithm, but architected around what LunarArc can actually intercept
 * rather than ported verbatim.
 *
 * <p>Real Paper threads a per-send {@code ChunkPacketInfo} context through source-patched
 * {@code PalettedContainer}/{@code LevelChunkSection}/{@code ChunkSerializer} constructors so
 * replacement blocks are baked into a chunk's palette at load time and swapped by raw bit index at
 * send time - a micro-optimisation only possible because Paper patches those classes' source
 * directly. LunarArc mixes into an unpatched vanilla/NeoForge runtime instead, so this rebuilds only
 * the observable behaviour: a fresh, temporary, obfuscated copy of a chunk's below-height-limit
 * sections is built on demand, immediately before that chunk's packet is serialised for sending (see
 * the mixin on {@code ClientboundLevelChunkPacketData}), and discarded right after - the persisted
 * chunk and its real block states are never touched. This costs a 4096-block copy per obfuscated
 * section instead of a bit swap, but only runs once per chunk a player newly sees, not per tick, and
 * cannot corrupt the world's actual data the way patching the live palette in place could if gotten
 * wrong.</p>
 *
 * <p>Only engine-mode 1 (HIDE) is implemented - the mode {@code CraftServer}'s generated default
 * already selects. Modes 2/3 (OBFUSCATE / OBFUSCATE_LAYER), which pick a per-position decoy from the
 * hidden-blocks list itself rather than one fixed replacement, are not; selecting either in config
 * disables anti-xray with a log message rather than silently behaving like HIDE.</p>
 *
 * <p>The reveal trigger and its neighbour set are real Paper's own algorithm
 * ({@code ChunkPacketBlockControllerAntiXray#onBlockChange}/{@code #updateNearbyBlocks}/
 * {@code #updateBlock}): a solid block turning non-solid - mining exposes a cavity - rechecks a fixed
 * set of positions up to two orthogonal steps away, and any of them still holding a hidden block is
 * marked dirty so the chunk system resends its real state to every player already tracking that
 * chunk. No custom packet-sending code is needed for the reveal itself - {@code blockChanged(pos)} is
 * the same vanilla primitive an ordinary block update already uses.</p>
 */
public final class LunarArcAntiXrayEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(LunarArcAntiXrayEngine.class);
    private static final java.util.Map<ServerLevel, LunarArcAntiXrayEngine> ENGINES = new ConcurrentHashMap<>();
    private static final Set<Block> SOLID_EXEMPT = Set.of(
            Blocks.SPAWNER, Blocks.BARRIER, Blocks.SHULKER_BOX, Blocks.SLIME_BLOCK, Blocks.MANGROVE_ROOTS);

    private final boolean enabled;
    private final int maxBlockHeight;
    private final int updateRadius;
    private final boolean lavaObscures;
    private final Set<BlockState> hiddenStates;

    private LunarArcAntiXrayEngine(ServerLevel level) {
        YamlConfiguration config = new YamlConfiguration();
        File file = new File("config", "paper-world-defaults.yml");
        boolean loaded = false;
        if (file.isFile()) {
            try {
                config.load(file);
                loaded = true;
            } catch (Exception ex) {
                LOGGER.warn("Unable to read {} for anti-xray - treating anti-xray as disabled for {}",
                        file.getPath(), level.dimension().location(), ex);
            }
        }

        boolean configuredEnabled = loaded && config.getBoolean("anticheat.anti-xray.enabled", false);
        int engineMode = config.getInt("anticheat.anti-xray.engine-mode", 1);
        if (configuredEnabled && engineMode != 1) {
            LOGGER.warn("anticheat.anti-xray.engine-mode {} is not implemented yet (only 1/HIDE is) "
                    + "- anti-xray is disabled for {}", engineMode, level.dimension().location());
            configuredEnabled = false;
        }

        this.enabled = configuredEnabled;
        this.maxBlockHeight = config.getInt("anticheat.anti-xray.max-block-height", 64);
        this.updateRadius = config.getInt("anticheat.anti-xray.update-radius", 2);
        this.lavaObscures = config.getBoolean("anticheat.anti-xray.lava-obscures", false);

        Set<BlockState> states = new HashSet<>();
        if (this.enabled) {
            for (String id : config.getStringList("anticheat.anti-xray.hidden-blocks")) {
                ResourceLocation location = ResourceLocation.tryParse(id);
                Block block = location == null ? null : BuiltInRegistries.BLOCK.get(location);
                if (block == null || block.defaultBlockState().isAir()) continue;
                states.addAll(block.getStateDefinition().getPossibleStates());
            }
            if (states.isEmpty()) {
                LOGGER.warn("anticheat.anti-xray.hidden-blocks resolved to no real blocks - "
                        + "anti-xray is enabled but has nothing to hide for {}", level.dimension().location());
            }
        }
        this.hiddenStates = states;

        if (this.enabled) {
            LOGGER.info("Anti-xray HIDE engine active for {}: {} hidden block state(s), "
                            + "max-block-height={}, update-radius={}",
                    level.dimension().location(), states.size(), maxBlockHeight, updateRadius);
        }
    }

    public static LunarArcAntiXrayEngine forLevel(ServerLevel level) {
        return ENGINES.computeIfAbsent(level, LunarArcAntiXrayEngine::new);
    }

    /** Clears the cached engine for a level so a config edit takes effect on its next (re)load. */
    public static void invalidate(ServerLevel level) {
        ENGINES.remove(level);
    }

    public boolean isEnabled() {
        return enabled;
    }

    private boolean isHidden(BlockState state) {
        return hiddenStates.contains(state);
    }

    private boolean isSolidForReveal(BlockState state) {
        if (state.isAir()) return false;
        if (lavaObscures && state == Blocks.LAVA.defaultBlockState()) return true;
        if (SOLID_EXEMPT.contains(state.getBlock())) return false;
        return state.blocksMotion();
    }

    private BlockState replacementFor(Level level, int sectionBottomY) {
        return switch (LunarArcDynamicBukkitEnums.environment(level.dimension().location())) {
            case NETHER -> Blocks.NETHERRACK.defaultBlockState();
            case THE_END -> Blocks.END_STONE.defaultBlockState();
            default -> sectionBottomY < 0 ? Blocks.DEEPSLATE.defaultBlockState() : Blocks.STONE.defaultBlockState();
        };
    }

    /**
     * Builds a temporary, obfuscated copy of {@code chunk}'s sections for sending over the network.
     * Returns the chunk's own real section array unchanged when disabled, or when none of its
     * sections fall under {@code max-block-height} - the common case above ground, where this must
     * add no cost at all.
     */
    public LevelChunkSection[] obfuscateForSend(LevelChunk chunk) {
        LevelChunkSection[] original = chunk.getSections();
        if (!enabled) return original;

        Level level = chunk.getLevel();
        int minBuildHeight = level.getMinBuildHeight();
        LevelChunkSection[] result = null;

        for (int i = 0; i < original.length; i++) {
            LevelChunkSection section = original[i];
            if (section == null) continue;
            int bottomY = (i << 4) + minBuildHeight;
            if (bottomY >= maxBlockHeight) continue;

            LevelChunkSection obfuscated = obfuscateSection(section, level, bottomY);
            if (obfuscated == null) continue;
            if (result == null) result = original.clone();
            result[i] = obfuscated;
        }

        return result == null ? original : result;
    }

    private LevelChunkSection obfuscateSection(LevelChunkSection section, Level level, int bottomY) {
        BlockState replacement = replacementFor(level, bottomY);
        boolean changedAny = false;
        PalettedContainer<BlockState> states = new PalettedContainer<>(
                net.minecraft.world.level.block.Block.BLOCK_STATE_REGISTRY, Blocks.AIR.defaultBlockState(),
                PalettedContainer.Strategy.SECTION_STATES);

        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    BlockState real = section.getStates().get(x, y, z);
                    BlockState toWrite = isHidden(real) ? replacement : real;
                    if (toWrite != real) changedAny = true;
                    states.set(x, y, z, toWrite);
                }
            }
        }

        if (!changedAny) return null;
        return new LevelChunkSection(states, section.getBiomes());
    }

    public void onBlockChange(ServerLevel level, BlockPos pos, BlockState newState, BlockState oldState) {
        if (!enabled || oldState == null) return;
        if (!(isSolidForReveal(oldState) && !isSolidForReveal(newState))) return;
        if (pos.getY() > maxBlockHeight + updateRadius - 1) return;
        revealNear(level, pos);
    }

    public void onBlockInteractStart(ServerLevel level, BlockPos pos) {
        if (!enabled) return;
        if (pos.getY() > maxBlockHeight + updateRadius - 1) return;
        revealNear(level, pos);
    }

    private void revealNear(ServerLevel level, BlockPos pos) {
        for (BlockPos neighbor : neighborsWithinTwo(pos)) {
            BlockState state = level.getBlockStateIfLoaded(neighbor);
            if (state != null && isHidden(state)) {
                level.getChunkSource().blockChanged(neighbor);
            }
        }
    }

    private List<BlockPos> neighborsWithinTwo(BlockPos pos) {
        if (updateRadius <= 0) return List.of();
        if (updateRadius == 1) {
            return List.of(pos.west(), pos.east(), pos.below(), pos.above(), pos.north(), pos.south());
        }
        // updateRadius >= 2: real Paper's fixed "up to two orthogonal steps" neighbour set.
        BlockPos west = pos.west();
        BlockPos east = pos.east();
        BlockPos below = pos.below();
        BlockPos above = pos.above();
        return List.of(
                west, west.west(), west.below(), west.above(), west.north(), west.south(),
                east, east.east(), east.below(), east.above(), east.north(), east.south(),
                below, below.below(), below.north(), below.south(),
                above, above.above(), above.north(), above.south(),
                pos.north(), pos.north().north(),
                pos.south(), pos.south().south());
    }
}
