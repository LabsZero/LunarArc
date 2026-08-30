package io.ampznetwork.lunararc.common.bridge.access;

import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Normal runtime bridge implemented by the corresponding Mixin accessor/invoker. */
public interface ThrowableItemProjectileAccessBridge {
    Item lunararc$getDefaultItem();
}
