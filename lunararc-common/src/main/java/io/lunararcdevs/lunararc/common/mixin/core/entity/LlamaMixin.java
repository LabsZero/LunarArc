package io.lunararcdevs.lunararc.common.mixin.core.entity;

import net.minecraft.world.entity.animal.horse.Llama;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Llama.class)
public abstract class LlamaMixin {
    @Shadow private void setStrength(int strength) { throw new AssertionError(); }

    public void setStrengthPublic(int strength) {
        this.setStrength(strength);
    }
}
