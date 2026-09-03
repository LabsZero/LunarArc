package org.bukkit.craftbukkit.entity;
import org.bukkit.craftbukkit.CraftServer;
public final class CraftZombieHorse extends CraftAbstractHorse implements org.bukkit.entity.ZombieHorse {
    public CraftZombieHorse(CraftServer server, net.minecraft.world.entity.animal.horse.ZombieHorse entity) { super(server, entity); }
    @Override public net.minecraft.world.entity.animal.horse.ZombieHorse getHandle() { return (net.minecraft.world.entity.animal.horse.ZombieHorse) entity; }
    @Override public org.bukkit.entity.Horse.Variant getVariant() { return org.bukkit.entity.Horse.Variant.UNDEAD_HORSE; }
    @Override public String toString() { return "CraftZombieHorse"; }
}
