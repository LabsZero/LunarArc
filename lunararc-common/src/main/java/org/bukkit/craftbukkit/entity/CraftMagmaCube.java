package org.bukkit.craftbukkit.entity;
import org.bukkit.craftbukkit.CraftServer;
public final class CraftMagmaCube extends CraftSlime implements org.bukkit.entity.MagmaCube {
    public CraftMagmaCube(CraftServer server, net.minecraft.world.entity.monster.MagmaCube entity) { super(server, entity); }
    @Override public net.minecraft.world.entity.monster.MagmaCube getHandle() { return (net.minecraft.world.entity.monster.MagmaCube) entity; }
    @Override public String toString() { return "CraftMagmaCube"; }
}
