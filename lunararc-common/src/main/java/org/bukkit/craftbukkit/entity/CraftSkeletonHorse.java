package org.bukkit.craftbukkit.entity;
import org.bukkit.craftbukkit.CraftServer;
public final class CraftSkeletonHorse extends CraftAbstractHorse implements org.bukkit.entity.SkeletonHorse {
    public CraftSkeletonHorse(CraftServer server, net.minecraft.world.entity.animal.horse.SkeletonHorse entity) { super(server, entity); }
    @Override public net.minecraft.world.entity.animal.horse.SkeletonHorse getHandle() { return (net.minecraft.world.entity.animal.horse.SkeletonHorse) entity; }
    @Override public org.bukkit.entity.Horse.Variant getVariant() { return org.bukkit.entity.Horse.Variant.SKELETON_HORSE; }
    @Override public boolean isTrapped() { return getHandle().isTrap(); }
    @Override public void setTrapped(boolean trapped) { getHandle().setTrap(trapped); }
    @Override public int getTrapTime() { return getHandle().trapTime; }
    @Override public void setTrapTime(int trapTime) { getHandle().trapTime = trapTime; }
    @Override public boolean isTrap() { return isTrapped(); }
    @Override public void setTrap(boolean trap) { setTrapped(trap); }
    @Override public String toString() { return "CraftSkeletonHorse"; }
}
