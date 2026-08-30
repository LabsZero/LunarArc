package org.bukkit.craftbukkit.entity;
import org.bukkit.craftbukkit.CraftServer;
public final class CraftMule extends CraftChestedHorse implements org.bukkit.entity.Mule {
    public CraftMule(CraftServer server, net.minecraft.world.entity.animal.horse.Mule entity) { super(server, entity); }
    @Override public org.bukkit.entity.Horse.Variant getVariant() { return org.bukkit.entity.Horse.Variant.MULE; }
    @Override public String toString() { return "CraftMule"; }
}
