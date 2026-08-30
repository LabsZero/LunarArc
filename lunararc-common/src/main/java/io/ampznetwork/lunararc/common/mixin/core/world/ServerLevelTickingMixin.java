package io.ampznetwork.lunararc.common.mixin.core.world;

import com.llamalad7.mixinextras.sugar.Local;
import io.ampznetwork.lunararc.common.mod.server.LunarArcTickingTrackerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.server.level.ServerLevel.class)
public abstract class ServerLevelTickingMixin {

    @Inject(method = "tickNonPassenger", at = @At("HEAD"), require = 0)
    private void lunararc$pushTickingEntity(Entity entity, CallbackInfo ci) {
        LunarArcTickingTrackerImpl.pushEntity(entity);
    }

    @Inject(method = "tickNonPassenger", at = @At("RETURN"), require = 0)
    private void lunararc$popTickingEntity(Entity entity, CallbackInfo ci) {
        LunarArcTickingTrackerImpl.pop();
    }

    // Block entity tick tracking: real ServerLevel iterates its ticking block entities
    // internally. The @Local capture of the BlockEntity local works at runtime if the method
    // is present under its Mojang-mapped name; require = 0 means it silently does nothing
    // rather than crashing if the target isn't found at this mapping level.
    @Inject(
        method = "tickBlockEntities()V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/entity/BlockEntityTicker;tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;)V"
        ),
        require = 0
    )
    private void lunararc$pushTickingBlockEntity(CallbackInfo ci, @Local BlockEntity blockEntity) {
        LunarArcTickingTrackerImpl.pushBlockEntity(blockEntity);
    }

    @Inject(method = "tickBlockEntities()V", at = @At("RETURN"), require = 0)
    private void lunararc$popTickingBlockEntity(CallbackInfo ci) {
        LunarArcTickingTrackerImpl.pop();
    }
}
