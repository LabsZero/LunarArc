package io.lunararcdevs.lunararc.common.mixin.core.world;

import net.minecraft.world.level.storage.PrimaryLevelData;
import org.bukkit.Bukkit;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PrimaryLevelData.class)
public abstract class PrimaryLevelDataWeatherMixin {
    @Shadow private boolean raining;
    @Shadow private boolean thundering;
    @Shadow public abstract String getLevelName();

    @Inject(method = "setRaining(Z)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void lunararc$weatherChange(boolean raining, CallbackInfo ci) {
        if (this.raining == raining || Bukkit.getServer() == null) return;
        org.bukkit.World world = Bukkit.getWorld(this.getLevelName());
        if (world == null) return;
        WeatherChangeEvent event = new WeatherChangeEvent(world, raining);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            org.slf4j.LoggerFactory.getLogger("LunarArc").info(
                    "Rain change to {} in world '{}' was cancelled by a plugin listening to WeatherChangeEvent", raining, world.getName());
            ci.cancel();
        }
    }

    @Inject(method = "setThundering(Z)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void lunararc$thunderChange(boolean thundering, CallbackInfo ci) {
        if (this.thundering == thundering || Bukkit.getServer() == null) return;
        org.bukkit.World world = Bukkit.getWorld(this.getLevelName());
        if (world == null) return;
        ThunderChangeEvent event = new ThunderChangeEvent(world, thundering);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            org.slf4j.LoggerFactory.getLogger("LunarArc").info(
                    "Thunder change to {} in world '{}' was cancelled by a plugin listening to ThunderChangeEvent", thundering, world.getName());
            ci.cancel();
        }
    }
}
