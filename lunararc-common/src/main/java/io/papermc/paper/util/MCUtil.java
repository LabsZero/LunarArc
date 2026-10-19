package io.papermc.paper.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.bukkit.Location;
import io.papermc.paper.math.Position;

public final class MCUtil {
    private MCUtil() {}

    public static BlockPos toBlockPosition(Location location) {
        return new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public static BlockPos toBlockPos(Position position) {
        return new BlockPos(position.blockX(), position.blockY(), position.blockZ());
    }

    public static io.papermc.paper.math.BlockPosition toPosition(net.minecraft.core.Vec3i vec) {
        return Position.block(vec.getX(), vec.getY(), vec.getZ());
    }

    public static io.papermc.paper.math.FinePosition toPosition(net.minecraft.world.phys.Vec3 vec) {
        return Position.fine(vec.x, vec.y, vec.z);
    }

    public static Location toLocation(Level level, BlockPos position) {
        return new Location(io.lunararcdevs.lunararc.common.LunarArcServerAccess.getCraftWorld(level),
                position.getX(), position.getY(), position.getZ());
    }

    public static <A, M> java.util.List<A> transformUnmodifiable(final java.util.List<? extends M> nms, final java.util.function.Function<? super M, ? extends A> converter) {
        return java.util.Collections.unmodifiableList(com.google.common.collect.Lists.transform(nms, converter::apply));
    }

    public static <A, M> java.util.Collection<A> transformUnmodifiable(final java.util.Collection<? extends M> nms, final java.util.function.Function<? super M, ? extends A> converter) {
        return java.util.Collections.unmodifiableCollection(com.google.common.collect.Collections2.transform(nms, converter::apply));
    }

    public static <A, M, C extends java.util.Collection<M>> void addAndConvert(final C target, final java.util.Collection<A> toAdd, final java.util.function.Function<? super A, ? extends M> converter) {
        for (final A value : toAdd) {
            target.add(converter.apply(value));
        }
    }

    public static org.bukkit.inventory.ItemStack deserializeItem(final net.minecraft.nbt.CompoundTag tag) {
        if (tag.getStringOr("id", "minecraft:air").equals("minecraft:air")) {
            return org.bukkit.craftbukkit.inventory.CraftItemStack.asBukkitMirror(net.minecraft.world.item.ItemStack.EMPTY);
        }
        return org.bukkit.craftbukkit.inventory.CraftItemStack.asBukkitMirror(net.minecraft.world.item.ItemStack.CODEC.parse(
                org.bukkit.craftbukkit.CraftRegistry.getMinecraftRegistry().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), tag
        ).getOrThrow());
    }

    public static byte[] serializeTagToBytes(final net.minecraft.nbt.CompoundTag tag) {
        final java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
        try {
            net.minecraft.nbt.NbtIo.writeCompressed(tag, outputStream);
        } catch (final java.io.IOException ex) {
            throw new RuntimeException(ex);
        }
        return outputStream.toByteArray();
    }

    public static net.minecraft.nbt.CompoundTag deserializeTagFromBytes(final byte[] data) {
        try {
            return net.minecraft.nbt.NbtIo.readCompressed(new java.io.ByteArrayInputStream(data), net.minecraft.nbt.NbtAccounter.unlimitedHeap());
        } catch (final java.io.IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
