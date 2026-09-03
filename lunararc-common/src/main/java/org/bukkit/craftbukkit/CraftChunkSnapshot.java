package org.bukkit.craftbukkit;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.jetbrains.annotations.NotNull;


public final class CraftChunkSnapshot implements ChunkSnapshot {
    private final int x, z, minY, maxY;
    private final String worldName;
    private final long fullTime;
    private final BlockState[] states;
    private final Biome[] biomes;
    private final double[] temperatures;
    private final byte[] skyLight, blockLight;
    private final int[] highest;
    private final boolean[] sectionEmpty;

    private CraftChunkSnapshot(int x, int z, int minY, int maxY, String worldName, long fullTime,
            BlockState[] states, Biome[] biomes, double[] temperatures, byte[] skyLight, byte[] blockLight,
            int[] highest, boolean[] sectionEmpty) {
        this.x=x; this.z=z; this.minY=minY; this.maxY=maxY; this.worldName=worldName; this.fullTime=fullTime;
        this.states=states; this.biomes=biomes; this.temperatures=temperatures; this.skyLight=skyLight; this.blockLight=blockLight;
        this.highest=highest; this.sectionEmpty=sectionEmpty;
    }

    public static CraftChunkSnapshot empty(CraftWorld world, int x, int z, boolean includeBiome,
            boolean includeTemperature) {
        Objects.requireNonNull(world, "world");
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight();
        int height = maxY - minY;
        BlockState air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        BlockState[] states = new BlockState[16 * height * 16];
        java.util.Arrays.fill(states, air);
        Biome[] biomes = includeBiome || includeTemperature ? new Biome[states.length] : null;
        double[] temperatures = includeTemperature ? new double[states.length] : null;
        if (biomes != null) {
            Biome fallbackBiome = world.getBiome((x << 4) + 8, minY, (z << 4) + 8);
            java.util.Arrays.fill(biomes, fallbackBiome);
        }
        if (temperatures != null) {
            double fallbackTemperature = world.getTemperature((x << 4) + 8, minY, (z << 4) + 8);
            java.util.Arrays.fill(temperatures, fallbackTemperature);
        }
        int[] highest = new int[256];
        java.util.Arrays.fill(highest, minY);
        boolean[] sectionEmpty = new boolean[(height + 15) >> 4];
        java.util.Arrays.fill(sectionEmpty, true);
        return new CraftChunkSnapshot(x, z, minY, maxY, world.getName(), world.getFullTime(), states,
                biomes, temperatures, null, null, highest, sectionEmpty);
    }

    public static CraftChunkSnapshot capture(CraftChunk craft, boolean includeMaxY, boolean includeBiome,
            boolean includeTemperature, boolean includeLight) {
        CraftWorld world = (CraftWorld) craft.getWorld();
        LevelChunk chunk = craft.getHandle();
        int minY=world.getMinHeight(), maxY=world.getMaxHeight(), height=maxY-minY;
        BlockState[] states = new BlockState[16*height*16];
        Biome[] biomes = includeBiome || includeTemperature ? new Biome[states.length] : null;
        double[] temps = includeTemperature ? new double[states.length] : null;
        byte[] sky = includeLight ? new byte[states.length] : null;
        byte[] emitted = includeLight ? new byte[states.length] : null;
        int[] highest = includeMaxY ? new int[256] : null;
        int sections = (height + 15) >> 4;
        boolean[] empty = new boolean[sections];
        java.util.Arrays.fill(empty, true);
        for (int lx=0; lx<16; lx++) for (int lz=0; lz<16; lz++) {
            int top=minY;
            for (int y=minY; y<maxY; y++) {
                BlockPos pos = new BlockPos((craft.getX()<<4)+lx, y, (craft.getZ()<<4)+lz);
                BlockState state = chunk.getBlockState(pos);
                int idx=index(lx,y,lz,minY,height);
                states[idx]=state;
                if (!state.isAir()) { top=y; empty[(y-minY)>>4]=false; }
                if (biomes != null) biomes[idx]=world.getBiome(pos.getX(), y, pos.getZ());
                if (temps != null) temps[idx]=world.getTemperature(pos.getX(), y, pos.getZ());
                if (includeLight) {
                    sky[idx]=(byte)world.getHandle().getBrightness(LightLayer.SKY,pos);
                    emitted[idx]=(byte)world.getHandle().getBrightness(LightLayer.BLOCK,pos);
                }
            }
            if (highest != null) highest[(lz<<4)|lx]=top;
        }
        return new CraftChunkSnapshot(craft.getX(),craft.getZ(),minY,maxY,world.getName(),world.getFullTime(),states,biomes,temps,sky,emitted,highest,empty);
    }

    private static int index(int x,int y,int z,int minY,int height) {
        if (x<0||x>15||z<0||z>15||y<minY||y>=minY+height) throw new IllegalArgumentException("snapshot coordinate out of range");
        return ((x*16)+z)*height+(y-minY);
    }
    private int idx(int x,int y,int z){ return index(x,y,z,minY,maxY-minY); }

    @Override public int getX(){return x;}
    @Override public int getZ(){return z;}
    @Override public @NotNull String getWorldName(){return worldName;}
    @Override public @NotNull Material getBlockType(int x,int y,int z){return CraftBlockData.createData(states[idx(x,y,z)]).getMaterial();}
    @Override public @NotNull BlockData getBlockData(int x,int y,int z){return CraftBlockData.createData(states[idx(x,y,z)]);}
    @Override public int getData(int x,int y,int z){return CraftMagicNumbers.toLegacyData(states[idx(x,y,z)]);}
    @Override public int getBlockSkyLight(int x,int y,int z){if(skyLight==null) throw new IllegalStateException("ChunkSnapshot created without light data. Please call getSnapshot with includeLightData=true"); return skyLight[idx(x,y,z)]&15;}
    @Override public int getBlockEmittedLight(int x,int y,int z){if(blockLight==null) throw new IllegalStateException("ChunkSnapshot created without light data. Please call getSnapshot with includeLightData=true"); return blockLight[idx(x,y,z)]&15;}
    @Override public int getHighestBlockYAt(int x,int z){if(highest==null) throw new IllegalStateException("ChunkSnapshot created without height map. Please call getSnapshot with includeMaxblocky=true"); if(x<0||x>15||z<0||z>15) throw new IllegalArgumentException("snapshot coordinate out of range"); return highest[(z<<4)|x];}
    @Override public @NotNull Biome getBiome(int x,int z){return getBiome(x,0,z);}
    @Override public @NotNull Biome getBiome(int x,int y,int z){if(biomes==null) throw new IllegalStateException("Biome data was not included in this snapshot"); return biomes[idx(x,y,z)];}
    @Override public double getRawBiomeTemperature(int x,int z){return getRawBiomeTemperature(x,0,z);}
    @Override public double getRawBiomeTemperature(int x,int y,int z){if(temperatures==null) throw new IllegalStateException("Biome temperature data was not included in this snapshot"); return temperatures[idx(x,y,z)];}
    @Override public long getCaptureFullTime(){return fullTime;}
    @Override public boolean isSectionEmpty(int sy){if(sy<0||sy>=sectionEmpty.length) throw new IllegalArgumentException("section out of range"); return sectionEmpty[sy];}
    @Override public boolean contains(@NotNull BlockData block){Objects.requireNonNull(block,"block"); if(!(block instanceof CraftBlockData c)) return false; for(BlockState s:states) if(s.equals(c.getState())) return true; return false;}
    @Override public boolean contains(@NotNull Biome biome){Objects.requireNonNull(biome,"biome"); if(biomes==null) return false; for(Biome b:biomes) if(b==biome) return true; return false;}
}
