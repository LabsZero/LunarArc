package io.lunararcdevs.lunararc.common.mixin.core.entity;

import io.lunararcdevs.lunararc.common.bridge.entity.ZombieBridge;
import net.minecraft.world.entity.monster.zombie.Zombie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Zombie.class)
public abstract class ZombieMixin implements ZombieBridge {
    @Unique private boolean lunararc$shouldBurnInDay = true;

    // Drowning conversion state lives in the zombie's drowningTracker (made accessible by the access widener).
    @Override public int lunararc$getConversionTime() { return ((Zombie) (Object) this).drowningTracker.conversionTime; }
    @Override public void lunararc$setConversionTimeDirect(int ticks) { ((Zombie) (Object) this).drowningTracker.conversionTime = ticks; }
    @Override public void lunararc$stopDrowning() {
        Zombie self = (Zombie) (Object) this;
        self.drowningTracker.conversionTime = -1;
        self.drowningTracker.setConverting(false);
    }
    @Override public boolean lunararc$shouldBurnInDay() { return this.lunararc$shouldBurnInDay; }
    @Override public void lunararc$setShouldBurnInDay(boolean burn) { this.lunararc$shouldBurnInDay = burn; }

    @Inject(method = "isSunSensitive", at = @At("HEAD"), cancellable = true)
    private void lunararc$paperBurnInDay(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(this.lunararc$shouldBurnInDay);
    }
}
