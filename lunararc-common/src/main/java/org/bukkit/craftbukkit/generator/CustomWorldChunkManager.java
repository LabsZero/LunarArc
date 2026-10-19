package org.bukkit.craftbukkit.generator;

import com.google.common.base.Preconditions;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import org.bukkit.block.Biome;
import org.bukkit.craftbukkit.block.CraftBiome;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.WorldInfo;

import java.util.List;
import java.util.stream.Stream;

public final class CustomWorldChunkManager extends BiomeSource {
    private final WorldInfo worldInfo;
    private final BiomeProvider provider;
    private final Registry<net.minecraft.world.level.biome.Biome> registry;
    private final List<Holder<net.minecraft.world.level.biome.Biome>> possible;
    public final BiomeSource vanillaBiomeSource;

    public CustomWorldChunkManager(WorldInfo worldInfo, BiomeProvider provider,
                                   Registry<net.minecraft.world.level.biome.Biome> registry,
                                   BiomeSource vanillaBiomeSource) {
        this.worldInfo = worldInfo;
        this.provider = provider;
        this.registry = registry;
        this.vanillaBiomeSource = vanillaBiomeSource;
        this.possible = provider.getBiomes(worldInfo).stream().map(CraftBiome::bukkitToMinecraftHolder).toList();
        Preconditions.checkArgument(!this.possible.isEmpty(), "BiomeProvider must provide at least one biome");
    }

    @Override protected MapCodec<? extends BiomeSource> codec() {
        throw new UnsupportedOperationException("Custom Bukkit BiomeProvider is runtime-only and cannot be serialized");
    }

    @Override
    public net.minecraft.world.level.biome.BiomeResolver createResolver(final Climate.Sampler sampler) {
        return (quartX, quartY, quartZ) -> {
            int x = net.minecraft.core.QuartPos.toBlock(quartX);
            int y = net.minecraft.core.QuartPos.toBlock(quartY);
            int z = net.minecraft.core.QuartPos.toBlock(quartZ);
            Biome biome = this.provider.getBiome(this.worldInfo, x, y, z,
                    CraftBiomeParameterPoint.createBiomeParameterPoint(sampler, sampler.sample(quartX, quartY, quartZ)));
            Preconditions.checkNotNull(biome, "BiomeProvider returned null at %s,%s,%s", x, y, z);
            return CraftBiome.bukkitToMinecraftHolder(biome);
        };
    }

    @Override protected Stream<Holder<net.minecraft.world.level.biome.Biome>> collectPossibleBiomes() {
        return this.possible.stream();
    }
}
