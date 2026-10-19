package io.lunararcdevs.lunararc.common.mixin.core.server;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.lunararcdevs.lunararc.common.LunarArcServerAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.WeatherData;
import org.bukkit.Bukkit;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerWeatherEventsMixin {
    @WrapOperation(method = "setWeatherParameters", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/saveddata/WeatherData;setRaining(Z)V"))
    private void lunararc$commandRain(WeatherData data, boolean raining, Operation<Void> original) {
        org.bukkit.World world = lunararc$overworld();
        if (world != null && data.isRaining() != raining) {
            WeatherChangeEvent event = new WeatherChangeEvent(world, raining, WeatherChangeEvent.Cause.COMMAND);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) return;
        }
        original.call(data, raining);
    }

    @WrapOperation(method = "setWeatherParameters", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/saveddata/WeatherData;setThundering(Z)V"))
    private void lunararc$commandThunder(WeatherData data, boolean thundering, Operation<Void> original) {
        org.bukkit.World world = lunararc$overworld();
        if (world != null && data.isThundering() != thundering) {
            ThunderChangeEvent event = new ThunderChangeEvent(world, thundering, ThunderChangeEvent.Cause.COMMAND);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) return;
        }
        original.call(data, thundering);
    }

    private org.bukkit.World lunararc$overworld() {
        if (Bukkit.getServer() == null) return null;
        net.minecraft.server.level.ServerLevel overworld = ((MinecraftServer) (Object) this).overworld();
        return overworld == null ? null : LunarArcServerAccess.getCraftWorld(overworld);
    }
}
