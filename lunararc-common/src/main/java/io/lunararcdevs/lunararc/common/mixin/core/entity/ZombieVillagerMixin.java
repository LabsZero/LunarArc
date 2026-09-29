package io.lunararcdevs.lunararc.common.mixin.core.entity;

import net.minecraft.world.entity.monster.ZombieVillager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.UUID;

@Mixin(ZombieVillager.class)
public abstract class ZombieVillagerMixin {
    @Shadow private void startConverting(UUID conversionStarter, int conversionTime) { throw new AssertionError(); }

    public void startConverting(UUID conversionStarter, int conversionTime, boolean broadcast) {
        this.startConverting(conversionStarter, conversionTime);
    }
}
