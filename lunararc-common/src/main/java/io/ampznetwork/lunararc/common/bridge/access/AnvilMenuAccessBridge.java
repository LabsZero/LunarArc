package io.ampznetwork.lunararc.common.bridge.access;

import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Normal runtime bridge implemented by the corresponding Mixin accessor/invoker. */
public interface AnvilMenuAccessBridge {
    String lunararc$getItemName();
    int lunararc$getRepairItemCountCost();
    void lunararc$setRepairItemCountCost(int value);
    DataSlot lunararc$getCost();
}
