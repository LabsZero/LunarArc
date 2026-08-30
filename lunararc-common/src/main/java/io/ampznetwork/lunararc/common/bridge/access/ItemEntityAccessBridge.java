package io.ampznetwork.lunararc.common.bridge.access;

import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.UUID;

/** Normal runtime bridge implemented by the corresponding Mixin accessor/invoker. */
public interface ItemEntityAccessBridge {
    int lunararc$getAge();
    void lunararc$setAge(int age);
    int lunararc$getPickupDelay();
    void lunararc$setPickupDelay(int delay);
    int lunararc$getHealth();
    void lunararc$setHealth(int health);
    UUID lunararc$getTarget();
    UUID lunararc$getThrower();
    void lunararc$setThrower(UUID thrower);
}
