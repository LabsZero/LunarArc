package io.lunararcdevs.lunararc.common.mixin.core.world.blockentity;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.SculkSensorBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SculkSensorBlockEntity.class)
public abstract class SculkSensorBlockEntityMixin implements io.lunararcdevs.lunararc.common.bridge.world.SculkRangeBridge {
    private static final String RANGE_KEY = "Paper.ListenerRange";

    public Integer rangeOverride;

    @Override
    public Integer lunararc$rangeOverride() {
        return this.rangeOverride;
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void lunararc$loadRange(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        this.rangeOverride = tag.contains(RANGE_KEY) ? Integer.valueOf(tag.getInt(RANGE_KEY)) : null;
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void lunararc$saveRange(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        if (this.rangeOverride != null && this.rangeOverride != 8) tag.putInt(RANGE_KEY, this.rangeOverride);
    }
}
