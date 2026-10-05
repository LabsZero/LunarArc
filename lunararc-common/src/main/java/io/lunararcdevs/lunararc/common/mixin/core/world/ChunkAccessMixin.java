package io.lunararcdevs.lunararc.common.mixin.core.world;

import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChunkAccess.class)
public abstract class ChunkAccessMixin {
    @Shadow public abstract int getMinBuildHeight();
    @Shadow public abstract int getHeight();
    @Shadow public abstract LevelChunkSection[] getSections();

    @SuppressWarnings("unchecked")
    public void setBiome(int x, int y, int z, Holder<Biome> biome) {
        int minQuart = QuartPos.fromBlock(this.getMinBuildHeight());
        int maxQuart = minQuart + QuartPos.fromBlock(this.getHeight()) - 1;
        int clamped = Mth.clamp(y, minQuart, maxQuart);
        LevelChunkSection section = this.getSections()[net.minecraft.core.SectionPos.blockToSectionCoord(QuartPos.toBlock(clamped)) - net.minecraft.core.SectionPos.blockToSectionCoord(this.getMinBuildHeight())];
        if (section.getBiomes() instanceof PalettedContainer<?> container) {
            ((PalettedContainer<Holder<Biome>>) container).set(x & 3, clamped & 3, z & 3, biome);
        }
    }
}
