package org.bukkit.craftbukkit.entity;

import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.DragonFireball;

public final class CraftDragonFireball extends CraftFireball implements DragonFireball {
    public CraftDragonFireball(CraftServer server, net.minecraft.world.entity.projectile.DragonFireball entity) { super(server, entity); }
    @Override public net.minecraft.world.entity.projectile.DragonFireball getHandle() { return (net.minecraft.world.entity.projectile.DragonFireball) this.entity; }
}
