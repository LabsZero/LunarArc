package io.lunararcdevs.lunararc.common.compat.worldedit;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.SavedDataStorage;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * Stand-in for the Paper-constructed temporary world WorldEdit builds for {@code //regen}. WorldEdit's adapters call a
 * Paper-specific {@code ServerLevel} constructor, which does not exist on the loader's vanilla {@code ServerLevel};
 * the integrated patcher redirects that call here and this class forwards to the vanilla constructor.
 */
public final class LunarArcWorldEditRegenLevel extends ServerLevel {

    /** Pre-26.x adapters (1.21.x): Paper's constructor took the level data directly. */
    public LunarArcWorldEditRegenLevel(
            MinecraftServer server,
            Executor dispatcher,
            LevelStorageSource.LevelStorageAccess access,
            net.minecraft.world.level.storage.ServerLevelData levelData,
            ResourceKey<Level> dimension,
            LevelStem levelStem,
            boolean isDebug,
            long seed,
            List<CustomSpawner> customSpawners,
            boolean tickTime,
            org.bukkit.World.Environment env,
            org.bukkit.generator.ChunkGenerator gen,
            org.bukkit.generator.BiomeProvider biomeProvider) {
        super(server, dispatcher, access, levelData, dimension, levelStem,
                isDebug, seed, customSpawners, tickTime);
        rejectCustomGenerators(gen, biomeProvider);
    }

    /**
     * 26.x adapters (WorldEdit {@code v26_*}): Paper's constructor takes the world's {@code WorldGenSettings}, the stem
     * key, a {@code SavedDataStorage} and the loaded world data. A throwaway world has no use for the last two, since
     * the vanilla constructor builds its own storage from {@code access}.
     */
    public LunarArcWorldEditRegenLevel(
            MinecraftServer server,
            Executor dispatcher,
            LevelStorageSource.LevelStorageAccess access,
            WorldGenSettings worldGenSettings,
            ResourceKey<Level> dimension,
            LevelStem levelStem,
            boolean isDebug,
            long seed,
            List<CustomSpawner> customSpawners,
            boolean tickTime,
            ResourceKey<LevelStem> stemKey,
            org.bukkit.World.Environment env,
            org.bukkit.generator.ChunkGenerator gen,
            org.bukkit.generator.BiomeProvider biomeProvider,
            SavedDataStorage savedDataStorage,
            io.papermc.paper.world.PaperWorldLoader.LoadedWorldData loadedWorldData) {
        super(server, dispatcher, access,
                new DerivedLevelData(server.getWorldData(), server.getWorldData().overworldData()),
                dimension, levelStem, isDebug, seed, customSpawners, tickTime);
        rejectCustomGenerators(gen, biomeProvider);
    }

    private static void rejectCustomGenerators(org.bukkit.generator.ChunkGenerator gen, org.bukkit.generator.BiomeProvider biomeProvider) {
        if (gen != null || biomeProvider != null) {
            throw new UnsupportedOperationException(
                    "LunarArc cannot regenerate a region whose world uses a custom Bukkit "
                            + "ChunkGenerator/BiomeProvider plugin yet - only plain/vanilla-mapped "
                            + "world generation is supported for //regen right now.");
        }
    }
}
