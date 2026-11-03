package io.lunararcdevs.lunararc.common.mixin.core.world;

import io.lunararcdevs.lunararc.common.server.LunarArcServerSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BaseSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BaseSpawner.class)
public abstract class BaseSpawnerRateMixin {
    @Inject(method = "serverTick", at = @At("HEAD"), cancellable = true, require = 0)
    private void lunararc$tickRate(ServerLevel level, BlockPos pos, CallbackInfo ci) {
        if (!LunarArcServerSettings.tickAllowed(level, pos, LunarArcServerSettings.tuning(level).mobSpawnerRate)) ci.cancel();
    }
}
