package io.ampznetwork.lunararc.common.mixin.core.entity;

import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ThrownPotion.class)
public interface ThrownPotionInvoker extends io.ampznetwork.lunararc.common.bridge.access.ThrownPotionInvokeBridge {
    @Invoker("onHit")
    void lunararc$invokeOnHit(HitResult result);
}
