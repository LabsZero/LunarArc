package io.lunararcdevs.lunararc.common.mixin.core.world;

import io.lunararcdevs.lunararc.common.event.LunarArcMoreEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class BlockBreakProgressEventsMixin {

    @Inject(method = "destroyBlockProgress", at = @At("HEAD"), require = 0)
    private void lunararc$progress(int entityId, BlockPos pos, int progress, CallbackInfo ci) {
        LunarArcMoreEvents.breakProgress((ServerLevel) (Object) this, entityId, pos, progress);
    }
}
