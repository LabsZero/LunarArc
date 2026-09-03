package io.ampznetwork.lunararc.common.bridge.access;

import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Normal runtime bridge implemented by the corresponding Mixin accessor/invoker. */
public interface ThrownPotionInvokeBridge {
    void lunararc$invokeOnHit(HitResult result);
}
