package io.lunararcdevs.lunararc.common.mixin.core.entity;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.lunararcdevs.lunararc.common.event.LunarArcMoreEvents;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Enderman.class)
public abstract class MoreMobEvents_EndermanMixin {

    @ModifyReturnValue(method = "isBeingStaredBy", at = @At("RETURN"), require = 1)
    private boolean lunararc$looked(boolean looking, Player player) {
        return looking && LunarArcMoreEvents.endermanLooked((Enderman) (Object) this, player);
    }
}
