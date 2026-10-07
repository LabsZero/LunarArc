package io.lunararcdevs.lunararc.common.mixin.core.world.blockentity;

import io.lunararcdevs.lunararc.common.event.LunarArcMoreEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BellBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(BellBlockEntity.class)
public abstract class BellBlockEntityMixin {

    @Shadow private static boolean isRaiderWithinRange(BlockPos pos, LivingEntity entity) { throw new AssertionError(); }

    @Shadow private static void glow(LivingEntity entity) { throw new AssertionError(); }

    @Inject(method = "makeRaidersGlow", at = @At("HEAD"), cancellable = true, require = 0)
    private static void lunararc$glow(Level level, BlockPos pos, List<LivingEntity> entities, CallbackInfo ci) {
        if (!LunarArcMoreEvents.bellListened()) return;
        List<LivingEntity> raiders = entities.stream().filter(entity -> isRaiderWithinRange(pos, entity)).toList();
        for (LivingEntity raider : LunarArcMoreEvents.bellResonate(level, pos, raiders)) glow(raider);
        ci.cancel();
    }
}
