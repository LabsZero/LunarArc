package io.lunararcdevs.lunararc.common.mixin.core.world;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.lunararcdevs.lunararc.common.LunarArcServerAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.WeatherData;
import org.bukkit.Bukkit;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** WeatherChangeEvent / ThunderChangeEvent. 26.x keeps the weather in WeatherData, which the level cycles itself. */
@Mixin(ServerLevel.class)
public abstract class ServerLevelWeatherEventsMixin {
    @WrapOperation(method = "advanceWeatherCycle", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/saveddata/WeatherData;setRaining(Z)V"))
    private void lunararc$naturalRain(WeatherData data, boolean raining, Operation<Void> original) {
        if (lunararc$rain(data, raining, WeatherChangeEvent.Cause.NATURAL)) original.call(data, raining);
    }

    @WrapOperation(method = "advanceWeatherCycle", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/saveddata/WeatherData;setThundering(Z)V"))
    private void lunararc$naturalThunder(WeatherData data, boolean thundering, Operation<Void> original) {
        if (lunararc$thunder(data, thundering, ThunderChangeEvent.Cause.NATURAL)) original.call(data, thundering);
    }

    @WrapOperation(method = "resetWeatherCycle", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/saveddata/WeatherData;setRaining(Z)V"))
    private void lunararc$sleepRain(WeatherData data, boolean raining, Operation<Void> original) {
        if (lunararc$rain(data, raining, WeatherChangeEvent.Cause.SLEEP)) original.call(data, raining);
    }

    @WrapOperation(method = "resetWeatherCycle", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/saveddata/WeatherData;setThundering(Z)V"))
    private void lunararc$sleepThunder(WeatherData data, boolean thundering, Operation<Void> original) {
        if (lunararc$thunder(data, thundering, ThunderChangeEvent.Cause.SLEEP)) original.call(data, thundering);
    }

    private boolean lunararc$rain(WeatherData data, boolean raining, WeatherChangeEvent.Cause cause) {
        if (data.isRaining() == raining || Bukkit.getServer() == null) return true;
        org.bukkit.World world = LunarArcServerAccess.getCraftWorld((ServerLevel) (Object) this);
        if (world == null) return true;
        WeatherChangeEvent event = new WeatherChangeEvent(world, raining, cause);
        Bukkit.getPluginManager().callEvent(event);
        return !event.isCancelled();
    }

    private boolean lunararc$thunder(WeatherData data, boolean thundering, ThunderChangeEvent.Cause cause) {
        if (data.isThundering() == thundering || Bukkit.getServer() == null) return true;
        org.bukkit.World world = LunarArcServerAccess.getCraftWorld((ServerLevel) (Object) this);
        if (world == null) return true;
        ThunderChangeEvent event = new ThunderChangeEvent(world, thundering, cause);
        Bukkit.getPluginManager().callEvent(event);
        return !event.isCancelled();
    }
}
