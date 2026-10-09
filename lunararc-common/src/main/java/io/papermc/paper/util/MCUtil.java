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
}
