package io.lunararcdevs.lunararc.common.mixin.core.world.blockentity;

import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(BannerBlockEntity.class)
public abstract class BannerBlockEntityMixin {
    @Shadow private BannerPatternLayers patterns;

    public void setPatterns(BannerPatternLayers layers) {
        if (layers.layers().size() > 20) {
            layers = new BannerPatternLayers(List.copyOf(layers.layers().subList(0, 20)));
        }
        this.patterns = layers;
    }
}
