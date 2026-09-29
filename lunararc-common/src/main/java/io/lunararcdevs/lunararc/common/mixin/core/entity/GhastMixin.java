package io.lunararcdevs.lunararc.common.mixin.core.entity;

import net.minecraft.world.entity.monster.Ghast;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Ghast.class)
public abstract class GhastMixin {
    @Shadow private int explosionPower;

    public void setExplosionPower(int explosionPower) {
        this.explosionPower = explosionPower;
    }
}
