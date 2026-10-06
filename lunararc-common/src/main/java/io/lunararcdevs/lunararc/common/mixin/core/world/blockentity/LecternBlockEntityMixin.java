package io.lunararcdevs.lunararc.common.mixin.core.world.blockentity;

import io.lunararcdevs.lunararc.common.bridge.donor.DonorContainerSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LecternBlockEntity.class)
public abstract class LecternBlockEntityMixin {
    @Shadow @Final public Container bookAccess;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void lunararc$registerBookAccess(BlockPos pos, BlockState state, CallbackInfo ci) {
        DonorContainerSupport.registerLectern(this.bookAccess, (LecternBlockEntity) (Object) this);
    }
}
