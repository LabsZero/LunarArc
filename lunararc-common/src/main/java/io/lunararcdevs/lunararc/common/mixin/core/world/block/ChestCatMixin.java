package io.lunararcdevs.lunararc.common.mixin.core.world.block;

import io.lunararcdevs.lunararc.common.server.LunarArcServerSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.ChestBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChestBlock.class)
public abstract class ChestCatMixin {
    @Inject(method = "isCatSittingOnChest", at = @At("HEAD"), cancellable = true, require = 0)
    private static void lunararc$noCatDetection(LevelAccessor level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (level instanceof net.minecraft.world.level.Level real && LunarArcServerSettings.tuning(real).disableChestCatDetection) cir.setReturnValue(false);
    }
}
