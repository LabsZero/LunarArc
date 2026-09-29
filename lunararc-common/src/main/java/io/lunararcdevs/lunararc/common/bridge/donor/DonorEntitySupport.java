package io.lunararcdevs.lunararc.common.bridge.donor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class DonorEntitySupport {
    private DonorEntitySupport() {}

    public static AABB itemFrameBox(BlockPos pos, Direction direction) {
        Vec3 center = Vec3.atCenterOf(pos).relative(direction, -0.46875D);
        Direction.Axis axis = direction.getAxis();
        double sizeX = axis == Direction.Axis.X ? 0.0625D : 0.75D;
        double sizeY = axis == Direction.Axis.Y ? 0.0625D : 0.75D;
        double sizeZ = axis == Direction.Axis.Z ? 0.0625D : 0.75D;
        return AABB.ofSize(center, sizeX, sizeY, sizeZ);
    }

    public static AABB paintingBox(BlockPos pos, Direction direction, int width, int height) {
        Vec3 center = Vec3.atCenterOf(pos).relative(direction, -0.46875D);
        double offsetX = width % 2 == 0 ? 0.5D : 0.0D;
        double offsetY = height % 2 == 0 ? 0.5D : 0.0D;
        Vec3 origin = center.relative(direction.getCounterClockWise(), offsetX).relative(Direction.UP, offsetY);
        Direction.Axis axis = direction.getAxis();
        double sizeX = axis == Direction.Axis.X ? 0.0625D : (double) width;
        double sizeZ = axis == Direction.Axis.Z ? 0.0625D : (double) width;
        return AABB.ofSize(origin, sizeX, (double) height, sizeZ);
    }
}
