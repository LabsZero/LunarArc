package io.lunararcdevs.lunararc.common.mixin.core.world.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ContainerOpenersCounter.class)
public abstract class ContainerOpenersCounterMixin {
    public boolean opened;

    @Shadow protected abstract void onOpen(Level level, BlockPos pos, BlockState state);
    @Shadow protected abstract void onClose(Level level, BlockPos pos, BlockState state);
    @Shadow protected abstract void openerCountChanged(Level level, BlockPos pos, BlockState state, int oldCount, int newCount);

    public void onAPIOpen(Level level, BlockPos pos, BlockState state) {
        this.onOpen(level, pos, state);
    }

    public void onAPIClose(Level level, BlockPos pos, BlockState state) {
        this.onClose(level, pos, state);
    }

    public void openerAPICountChanged(Level level, BlockPos pos, BlockState state, int oldCount, int newCount) {
        this.openerCountChanged(level, pos, state, oldCount, newCount);
    }

    @Inject(method = "recheckOpeners", at = @At("HEAD"), cancellable = true)
    private void lunararc$keepApiOpen(Level level, BlockPos pos, BlockState state, CallbackInfo ci) {
        if (this.opened) ci.cancel();
    }
}
