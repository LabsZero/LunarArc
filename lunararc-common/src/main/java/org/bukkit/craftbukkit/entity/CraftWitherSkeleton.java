package org.bukkit.craftbukkit.entity;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.WitherSkeleton;
public final class CraftWitherSkeleton extends CraftAbstractSkeleton implements WitherSkeleton {
    public CraftWitherSkeleton(CraftServer server, net.minecraft.world.entity.monster.WitherSkeleton entity) { super(server, entity); }
    @Override public net.minecraft.world.entity.monster.WitherSkeleton getHandle() { return (net.minecraft.world.entity.monster.WitherSkeleton) this.entity; }
    @Override public org.bukkit.entity.Skeleton.SkeletonType getSkeletonType() { return org.bukkit.entity.Skeleton.SkeletonType.WITHER; }
    @Override public String toString() { return "CraftWitherSkeleton"; }
}
