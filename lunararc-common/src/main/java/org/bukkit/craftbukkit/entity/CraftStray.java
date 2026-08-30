package org.bukkit.craftbukkit.entity;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Stray;
public final class CraftStray extends CraftAbstractSkeleton implements Stray {
    public CraftStray(CraftServer server, net.minecraft.world.entity.monster.Stray entity) { super(server, entity); }
    @Override public net.minecraft.world.entity.monster.Stray getHandle() { return (net.minecraft.world.entity.monster.Stray) this.entity; }
    @Override public org.bukkit.entity.Skeleton.SkeletonType getSkeletonType() { return org.bukkit.entity.Skeleton.SkeletonType.STRAY; }
    @Override public String toString() { return "CraftStray"; }
}
