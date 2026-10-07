package io.lunararcdevs.lunararc.common.mixin.core.world.raid;

import io.lunararcdevs.lunararc.common.event.LunarArcMoreEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raids;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(Raids.class)
public abstract class RaidsEventsMixin {
    @Shadow @Final private ServerLevel level;
    @Shadow private Map<Integer, Raid> raidMap;

    @Inject(method = "createOrExtendRaid", at = @At("RETURN"), cancellable = true, require = 0)
    private void lunararc$trigger(ServerPlayer player, BlockPos pos, CallbackInfoReturnable<Raid> cir) {
        Raid raid = cir.getReturnValue();
        if (raid == null || LunarArcMoreEvents.raidTrigger(raid, this.level, player)) return;
        this.raidMap.remove(raid.getId());
        player.removeEffect(MobEffects.RAID_OMEN);
        cir.setReturnValue(null);
    }
}
