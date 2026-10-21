package io.lunararcdevs.lunararc.common.mixin.core.events;

import io.lunararcdevs.lunararc.common.event.LunarArcGameplayEvents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** ItemTransportingEntitySortEvent: plugins decide whether an item-carrying mob may leave its items in a container. */
@Mixin(TransportItemsBetweenContainers.class)
public abstract class GameplayItemSortMixin {
    @Inject(method = "matchesLeavingItemsRequirement", at = @At("HEAD"), cancellable = true, require = 1)
    private static void lunararc$sort(PathfinderMob body, Container container, CallbackInfoReturnable<Boolean> cir) {
        Boolean verdict = LunarArcGameplayEvents.itemSort(body, container);
        if (verdict != null) cir.setReturnValue(verdict);
    }
}
