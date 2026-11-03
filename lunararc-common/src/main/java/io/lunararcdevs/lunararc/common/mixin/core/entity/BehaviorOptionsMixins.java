package io.lunararcdevs.lunararc.common.mixin.core.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.lunararcdevs.lunararc.common.server.LunarArcServerSettings;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public abstract class BehaviorOptionsMixins {
    @ModifyExpressionValue(method = "attack", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;onClimbable()Z"))
    private boolean lunararc$noPlayerCrits(boolean climbing) {
        Player self = (Player) (Object) this;
        return climbing || LunarArcServerSettings.tuning(self.level()).disablePlayerCrits;
    }
}
