package org.bukkit.craftbukkit.entity;

import net.minecraft.world.entity.PathfinderMob;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Creature;

/** Concrete Bukkit creature wrapper for the loader-owned 1.21.1 PathfinderMob. */
public class CraftCreature extends CraftMob implements Creature {
    public CraftCreature(CraftServer server, PathfinderMob entity) {
        super(server, entity);
    }

    @Override
    public PathfinderMob getHandle() {
        return (PathfinderMob) this.entity;
    }

    @Override
    public String toString() {
        return "CraftCreature";
    }
}
