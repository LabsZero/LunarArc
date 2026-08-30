package org.bukkit.craftbukkit.entity;
import org.bukkit.craftbukkit.CraftServer;
public final class CraftDonkey extends CraftChestedHorse implements org.bukkit.entity.Donkey {
    public CraftDonkey(CraftServer server, net.minecraft.world.entity.animal.horse.Donkey entity) { super(server, entity); }
    @Override public org.bukkit.entity.Horse.Variant getVariant() { return org.bukkit.entity.Horse.Variant.DONKEY; }
    @Override public String toString() { return "CraftDonkey"; }
}
