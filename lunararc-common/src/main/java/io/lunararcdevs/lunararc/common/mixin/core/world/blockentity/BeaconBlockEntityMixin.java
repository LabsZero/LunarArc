package io.lunararcdevs.lunararc.common.mixin.core.world.blockentity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.lunararcdevs.lunararc.common.bridge.world.BeaconRangeBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.potion.CraftPotionUtil;
import org.bukkit.potion.PotionEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BeaconBlockEntity.class)
public abstract class BeaconBlockEntityMixin implements BeaconRangeBridge {
    private static final String RANGE_KEY = "Paper.Range";

    @Unique private static double lunararc$activeRange = -1.0D;
    @Unique private double lunararc$effectRange = -1.0D;

    @Shadow int levels;
    @Shadow Holder<MobEffect> primaryPower;
    @Shadow Holder<MobEffect> secondaryPower;

    @Override
    public double lunararc$effectRange() {
        return this.getEffectRange();
    }

    public double getEffectRange() {
        return this.lunararc$effectRange < 0.0D ? this.levels * 10 + 10 : this.lunararc$effectRange;
    }

    public void setEffectRange(double range) {
        this.lunararc$effectRange = range;
    }

    public void resetEffectRange() {
        this.lunararc$effectRange = -1.0D;
    }

    public PotionEffect getPrimaryEffect() {
        if (this.primaryPower == null) return null;
        return CraftPotionUtil.toBukkit(new MobEffectInstance(this.primaryPower, (9 + this.levels * 2) * 20,
                lunararc$amplifier(this.levels, this.primaryPower, this.secondaryPower), true, true));
    }

    public PotionEffect getSecondaryEffect() {
        if (this.levels < 4 || this.primaryPower == this.secondaryPower || this.secondaryPower == null) return null;
        return CraftPotionUtil.toBukkit(new MobEffectInstance(this.secondaryPower, (9 + this.levels * 2) * 20,
                lunararc$amplifier(this.levels, this.primaryPower, this.secondaryPower), true, true));
    }

    @Unique
    private static int lunararc$amplifier(int levels, Holder<MobEffect> primary, Holder<MobEffect> secondary) {
        return levels >= 4 && primary == secondary ? 1 : 0;
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/entity/BeaconBlockEntity;applyEffects(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;ILnet/minecraft/core/Holder;Lnet/minecraft/core/Holder;)V"))
    private static void lunararc$rangeScope(Level level, BlockPos pos, int levels, Holder<MobEffect> primary,
            Holder<MobEffect> secondary, Operation<Void> original, Level tickLevel, BlockPos tickPos,
            BlockState state, BeaconBlockEntity beacon) {
        lunararc$activeRange = ((BeaconRangeBridge) beacon).lunararc$effectRange();
        try {
            original.call(level, pos, levels, primary, secondary);
        } finally {
            lunararc$activeRange = -1.0D;
        }
    }

    @ModifyArg(method = "applyEffects", index = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/phys/AABB;inflate(D)Lnet/minecraft/world/phys/AABB;"))
    private static double lunararc$inflate(double original) {
        return lunararc$activeRange >= 0.0D ? lunararc$activeRange : original;
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void lunararc$loadRange(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        this.lunararc$effectRange = tag.contains(RANGE_KEY) ? tag.getDouble(RANGE_KEY) : -1.0D;
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void lunararc$saveRange(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        if (this.lunararc$effectRange >= 0.0D) tag.putDouble(RANGE_KEY, this.lunararc$effectRange);
    }
}
