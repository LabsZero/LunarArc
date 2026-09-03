package io.ampznetwork.lunararc.common.mod.util;

import net.minecraft.core.BlockPos;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.AbstractList;
import java.util.List;

/**
 * The blocks a piston is about to move, as {@code BlockPistonEvent.getBlocks()} wants them.
 *
 * <p>CraftBukkit builds this as an anonymous {@code AbstractList} inside {@code moveBlocks}. It is
 * a top-level class here for one reason: the code that needs it lives in a mixin, and a list
 * declared inside a mixin would be an inner class for the mixin applicator to relocate into
 * {@code PistonBaseBlock}. That works, but it is a needless thing to ask of it when the list has
 * no reason to be there.</p>
 *
 * <p>The laziness is CraftBukkit's and is the point of the class rather than an optimisation
 * detail. A piston fires on every tick it is powered, on every piston on the server; resolving a
 * Bukkit Block per position up front would allocate for every one of them whether or not a single
 * plugin is listening. {@code get} resolves on demand, so a server with no piston listener pays for
 * two {@code size()} calls.</p>
 *
 * <p>Order is the moved blocks first, then the ones being destroyed, which is the order plugins
 * have always seen.</p>
 */
public final class LunarArcPistonAffectedBlocks extends AbstractList<Block> {

    private final World world;
    private final List<BlockPos> moved;
    private final List<BlockPos> broken;

    public LunarArcPistonAffectedBlocks(World world, List<BlockPos> moved, List<BlockPos> broken) {
        this.world = world;
        this.moved = moved;
        this.broken = broken;
    }

    @Override
    public int size() {
        return moved.size() + broken.size();
    }

    @Override
    public Block get(int index) {
        if (index >= size() || index < 0) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        BlockPos position = index < moved.size() ? moved.get(index) : broken.get(index - moved.size());
        return world.getBlockAt(position.getX(), position.getY(), position.getZ());
    }
}
