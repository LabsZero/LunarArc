package io.lunararcdevs.lunararc.common.mixin.core.world.blockentity;

import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShulkerBoxBlockEntity.class)
public abstract class ShulkerBoxBlockEntityMixin {
    public boolean opened;

    @Inject(method = {"startOpen", "stopOpen"}, cancellable = true,
            at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/world/level/Level;blockEvent(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;II)V"))
    private void lunararc$skipWhenApiOpened(net.minecraft.world.entity.player.Player player, CallbackInfo ci) {
        if (this.opened) ci.cancel();
    }
}
