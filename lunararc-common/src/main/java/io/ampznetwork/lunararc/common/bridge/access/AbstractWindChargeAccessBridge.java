package io.ampznetwork.lunararc.common.bridge.access;

import net.minecraft.world.entity.projectile.windcharge.AbstractWindCharge;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Normal runtime bridge implemented by the corresponding Mixin accessor/invoker. */
public interface AbstractWindChargeAccessBridge {
    void lunararc$invokeExplode(Vec3 position);
}
