package io.lunararcdevs.lunararc.common.mixin.core.world;

import io.lunararcdevs.lunararc.common.event.LunarArcMoreEvents;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventDispatcher;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameEventDispatcher.class)
public abstract class GameEventDispatcherEventsMixin {
    @Shadow @Final private ServerLevel level;

    @Inject(method = "post", at = @At("HEAD"), cancellable = true, require = 0)
    private void lunararc$generic(Holder<GameEvent> event, Vec3 position, GameEvent.Context context, CallbackInfo ci) {
        if (!LunarArcMoreEvents.gameEvent(event, position, context, this.level)) ci.cancel();
    }
}
