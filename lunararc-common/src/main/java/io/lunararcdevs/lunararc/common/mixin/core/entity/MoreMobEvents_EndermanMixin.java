package io.lunararcdevs.lunararc.common.mixin.core.entity;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.lunararcdevs.lunararc.common.event.LunarArcMoreEvents;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EnderMan.class)
public abstract class MoreMobEvents_EndermanMixin {

    @ModifyReturnValue(method = "isLookingAtMe", at = @At("RETURN"), require = 0)
    private boolean lunararc$looked(boolean looking, Player player) {
        return looking && LunarArcMoreEvents.endermanLooked((EnderMan) (Object) this, player);
    }
}
