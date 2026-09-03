package org.bukkit.craftbukkit.entity;

import io.ampznetwork.lunararc.common.bridge.LargeFireballBridge;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.LargeFireball;

public final class CraftLargeFireball extends CraftSizedFireball implements LargeFireball {
    public CraftLargeFireball(CraftServer server, net.minecraft.world.entity.projectile.LargeFireball entity) {
        super(server, entity);
        this.fireballBridge().lunararc$setBukkitYield(((LargeFireballBridge) (Object) entity).lunararc$getExplosionPower());
    }
    @Override public void setYield(float yield) {
        super.setYield(yield);
        ((LargeFireballBridge) (Object) getHandle()).lunararc$setExplosionPower((int) yield);
    }
    @Override public net.minecraft.world.entity.projectile.LargeFireball getHandle() { return (net.minecraft.world.entity.projectile.LargeFireball) this.entity; }
}
