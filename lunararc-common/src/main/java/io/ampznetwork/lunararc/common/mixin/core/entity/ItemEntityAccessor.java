package io.ampznetwork.lunararc.common.mixin.core.entity;

import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.UUID;


@Mixin(ItemEntity.class)
public interface ItemEntityAccessor extends io.ampznetwork.lunararc.common.bridge.access.ItemEntityAccessBridge {
    @Accessor("age") int lunararc$getAge();
    @Accessor("age") void lunararc$setAge(int age);

    @Accessor("pickupDelay") int lunararc$getPickupDelay();
    @Accessor("pickupDelay") void lunararc$setPickupDelay(int delay);

    @Accessor("health") int lunararc$getHealth();
    @Accessor("health") void lunararc$setHealth(int health);

    @Accessor("target") UUID lunararc$getTarget();

    @Accessor("thrower") UUID lunararc$getThrower();
    @Accessor("thrower") void lunararc$setThrower(UUID thrower);
}
