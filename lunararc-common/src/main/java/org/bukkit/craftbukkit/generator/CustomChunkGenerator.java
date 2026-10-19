package org.bukkit.craftbukkit.generator;

import com.mojang.serialization.MapCodec;
import io.lunararcdevs.lunararc.common.bridge.access.ChunkGeneratorAccessBridge;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.bukkit.craftbukkit.CraftHeightMap;
import org.bukkit.craftbukkit.util.RandomSourceWrapper;

/**
 * Concrete Bukkit ChunkGenerator adapter around the loader-owned NMS generator.
 * It never replaces loader lifecycle/registries; it only controls generation
 * stages according to Bukkit's ChunkGenerator contract. Minecraft 26.x builds terrain, surface and carvers in
 * one {@code buildTerrain} stage, so the vanilla delegate runs first (when the plugin allows noise) and the
 * Bukkit generator then layers its noise/surface/bedrock/caves on top of it.
 */
public final class CustomChunkGenerator extends InternalChunkGenerator {
    private static final long INITIAL_SEED = 0L;

    private final ChunkGenerator delegate;
    private final org.bukkit.generator.ChunkGenerator generator;
    private final CraftWorldInfo worldInfo;
    private volatile ServerLevel world;
    private boolean implementBaseHeight = true;

    public CustomChunkGenerator(ChunkGenerator delegate, org.bukkit.generator.ChunkGenerator generator,
                                CraftWorldInfo worldInfo, BiomeSource biomeSource) {
        super(biomeSource, ((ChunkGeneratorAccessBridge) (Object) delegate).lunararc$getGenerationSettingsGetter());
        this.delegate = delegate;
        this.generator = generator;
        this.worldInfo = worldInfo;
    }

    public void attachWorld(ServerLevel world) { this.world = world; }
    public ChunkGenerator getDelegate() { return this.delegate; }
    private ServerLevel world() {
        ServerLevel value = this.world;
        if (value == null) throw new IllegalStateException("CustomChunkGenerator used before ServerLevel attachment");
        return value;
    }
    private static WorldgenRandom seededRandom() { return new WorldgenRandom(new LegacyRandomSource(INITIAL_SEED)); }
    private java.util.Random random(WorldgenRandom random) { return new RandomSourceWrapper.RandomWrapper(random); }
    private CraftChunkData data(ChunkAccess chunk) { return new CraftChunkData(this.worldInfo.getMinHeight(), this.worldInfo.getMaxHeight(), chunk); }

    @Override public BiomeSource getBiomeSource() { return super.getBiomeSource(); }
    @Override public int getMinY() { return this.delegate.getMinY(); }
    @Override public int getSeaLevel() { return this.delegate.getSeaLevel(); }
    @Override public int getGenDepth() { return this.delegate.getGenDepth(); }
    @Override public int getSpawnHeight(LevelHeightAccessor level) { return this.delegate.getSpawnHeight(level); }

    @Override
    public void createStructures(RegistryAccess access, ChunkGeneratorStructureState state, StructureManager structures,
                                 ChunkAccess chunk, StructureTemplateManager templates, net.minecraft.resources.ResourceKey<Level> dimension) {
        if (this.generator == null) {
            this.delegate.createStructures(access, state, structures, chunk, templates, dimension);
            return;
        }
        WorldgenRandom random = seededRandom();
        int x = chunk.getPos().x(), z = chunk.getPos().z();
        random.setSeed(Mth.getSeed(x, "should-structures".hashCode(), z) ^ this.worldInfo.getSeed());
        if (this.generator.shouldGenerateStructures(this.worldInfo, random(random), x, z)) {
            this.delegate.createStructures(access, state, structures, chunk, templates, dimension);
        }
    }

    @Override
    public CompletableFuture<ChunkAccess> buildTerrain(ChunkAccess chunk, Blender blender, RandomState randomState,
                                                        StructureManager structures, BiomeManager biomes,
                                                        WorldGenRegion carverBiomeRegion, Set<Holder<net.minecraft.world.level.biome.Biome>> possibleBiomes) {
        if (this.generator == null) {
            return this.delegate.buildTerrain(chunk, blender, randomState, structures, biomes, carverBiomeRegion, possibleBiomes);
        }
        int x = chunk.getPos().x(), z = chunk.getPos().z();
        WorldgenRandom decision = seededRandom();
        decision.setSeed(Mth.getSeed(x, "should-noise".hashCode(), z) ^ this.worldInfo.getSeed());
        CompletableFuture<ChunkAccess> base = this.generator.shouldGenerateNoise(this.worldInfo, random(decision), x, z)
                ? this.delegate.buildTerrain(chunk, blender, randomState, structures, biomes, carverBiomeRegion, possibleBiomes)
                : CompletableFuture.completedFuture(chunk);
        return base.thenApply(result -> {
            CraftChunkData data = data(result);
            try {
                WorldgenRandom noise = seededRandom();
                noise.setLargeFeatureWithSalt(INITIAL_SEED, x, z, 0);
                this.generator.generateNoise(this.worldInfo, random(noise), x, z, data);

                WorldgenRandom surface = seededRandom();
                surface.setLargeFeatureWithSalt(INITIAL_SEED, x, z, 0);
                this.generator.generateSurface(this.worldInfo, random(surface), x, z, data);

                WorldgenRandom bedrock = seededRandom();
                bedrock.setLargeFeatureWithSalt(INITIAL_SEED, x, z, 0);
                this.generator.generateBedrock(this.worldInfo, random(bedrock), x, z, data);

                WorldgenRandom caves = seededRandom();
                caves.setDecorationSeed(this.worldInfo.getSeed(), 0, 0);
                this.generator.generateCaves(this.worldInfo, random(caves), x, z, data);
                data.applyTo(result);
                return result;
            } finally {
                data.breakLink();
            }
        });
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structures) {
        if (this.generator == null) {
            super.applyBiomeDecoration(level, chunk, structures);
            io.lunararcdevs.lunararc.common.mod.util.LunarArcChunkPopulators.populate(level, chunk);
            return;
        }
        int x = chunk.getPos().x(), z = chunk.getPos().z();
        WorldgenRandom decision = seededRandom();
        decision.setSeed(Mth.getSeed(x, "should-decoration".hashCode(), z) ^ level.getSeed());
        if (this.generator.shouldGenerateDecorations(this.worldInfo, random(decision), x, z)) {
            super.applyBiomeDecoration(level, chunk, structures);
        }
        io.lunararcdevs.lunararc.common.mod.util.LunarArcChunkPopulators.populate(level, chunk);
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
        if (this.generator == null) { this.delegate.spawnOriginalMobs(region); return; }
        int x = region.getCenter().x(), z = region.getCenter().z();
        WorldgenRandom decision = seededRandom();
        decision.setSeed(Mth.getSeed(x, "should-mobs".hashCode(), z) ^ region.getSeed());
        if (this.generator.shouldGenerateMobs(this.worldInfo, random(decision), x, z)) {
            this.delegate.spawnOriginalMobs(region);
        }
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState randomState) {
        if (this.generator != null && this.implementBaseHeight) {
            try {
                WorldgenRandom random = seededRandom();
                random.setLargeFeatureWithSalt(INITIAL_SEED, SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z), 0);
                return this.generator.getBaseHeight(this.worldInfo, random(random), x, z, CraftHeightMap.fromNMS(type));
            } catch (UnsupportedOperationException ignored) {
                this.implementBaseHeight = false;
            }
        }
        return this.delegate.getBaseHeight(x, z, type, level, randomState);
    }

    @Override public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState state) {
        return this.delegate.getBaseColumn(x, z, level, state);
    }

    @Override
    public WeightedList<MobSpawnSettings.SpawnerData> getMobsAt(Level level, StructureManager structures, MobCategory category, BlockPos pos) {
        return this.delegate.getMobsAt(level, structures, category, pos);
    }

    @Override
    public void addDebugScreenInfo(List<String> lines, RandomState state, BlockPos pos, SamplerContext samplerContext) {
        this.delegate.addDebugScreenInfo(lines, state, pos, samplerContext);
    }

    @Override protected MapCodec<? extends ChunkGenerator> codec() {
        throw new UnsupportedOperationException("Custom Bukkit ChunkGenerator is runtime-only and cannot be serialized");
    }
}
