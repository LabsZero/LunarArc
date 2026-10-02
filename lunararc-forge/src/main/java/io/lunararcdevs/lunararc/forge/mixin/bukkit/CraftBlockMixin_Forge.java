package io.lunararcdevs.lunararc.forge.mixin.bukkit;

import net.minecraft.world.level.biome.Biome;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = CraftBlock.class, remap = false)
public abstract class CraftBlockMixin_Forge {
    @Redirect(
            method = "getHumidity()D",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/level/biome/Biome;climateSettings:Lnet/minecraft/world/level/biome/Biome$ClimateSettings;",
                    remap = true))
    private Biome.ClimateSettings lunararc$useLoaderClimateSettings(Biome biome) {
        return biome.getModifiedClimateSettings();
    }
}
