package io.ampznetwork.lunararc.common.mixin.core.player;

import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla 1.20.5+ turned the food regen/starvation rates into {@code static final}
 * constants, but Paper re-adds them as instance fields so the deprecated
 * {@code HumanEntity#get/setSaturatedRegenRate} etc. API keeps working.
 * This mixin restores those fields (with Paper's default values) so the
 * {@link FoodDataAccessor} accessors can resolve against the target class.
 */
@Mixin(value = FoodData.class, priority = 2000)
public abstract class FoodDataMixin {

    @Unique
    public int saturatedRegenRate;

    @Unique
    public int unsaturatedRegenRate;

    @Unique
    public int starvationRate;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void lunararc$initRates(CallbackInfo ci) {
        this.saturatedRegenRate = 10;
        this.unsaturatedRegenRate = 80;
        this.starvationRate = 80;
    }
}
