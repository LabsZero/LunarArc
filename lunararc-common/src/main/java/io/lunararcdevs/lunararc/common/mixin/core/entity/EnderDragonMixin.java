package io.lunararcdevs.lunararc.common.mixin.core.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.levelgen.feature.EndPodiumFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EnderDragon.class)
public abstract class EnderDragonMixin {
    @Unique private BlockPos lunararc$podium;

    public BlockPos getPodium() {
        return this.lunararc$podium == null
                ? EndPodiumFeature.getLocation(((EnderDragon) (Object) this).getFightOrigin())
                : this.lunararc$podium;
    }

    public void setPodium(BlockPos podium) {
        this.lunararc$podium = podium;
    }
}
