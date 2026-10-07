package io.lunararcdevs.lunararc.common.mixin.core.world.blockentity;

import io.lunararcdevs.lunararc.common.event.LunarArcMoreEvents;
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
public abstract class LecternRegisterMixin {
    @Shadow @Final private Container bookAccess;

    @Inject(method = "<init>", at = @At("TAIL"), require = 0)
    private void lunararc$register(BlockPos pos, BlockState state, CallbackInfo ci) {
        LunarArcMoreEvents.registerLectern(this.bookAccess, (LecternBlockEntity) (Object) this);
    }
}
