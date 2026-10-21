package io.lunararcdevs.lunararc.common.mixin.core.events;

import io.lunararcdevs.lunararc.common.event.LunarArcGameplayEvents;
import java.util.function.Supplier;
import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * PlayerInventorySlotChangeEvent: reports a slot of the player's own inventory whose synchronised stack changed. Paper also lets the
 * event veto the "inventory changed" advancement trigger; LunarArc reports the change but leaves that trigger to vanilla.
 */
@Mixin(AbstractContainerMenu.class)
public abstract class GameplaySlotChangeMixin {
    @Shadow @Final private NonNullList<ItemStack> lastSlots;
    @Shadow @Final public NonNullList<net.minecraft.world.inventory.Slot> slots;

    @Unique private ItemStack lunararc$previousStack;

    @Inject(method = "triggerSlotListeners", at = @At("HEAD"), require = 1)
    private void lunararc$rememberStack(int slot, ItemStack expected, Supplier<ItemStack> actual, CallbackInfo ci) {
        this.lunararc$previousStack = null;
        if (slot < 0 || slot >= this.lastSlots.size()) return;
        ItemStack previous = this.lastSlots.get(slot);
        if (!ItemStack.matches(previous, expected)) this.lunararc$previousStack = previous.copy();
    }

    @Inject(method = "triggerSlotListeners", at = @At("RETURN"), require = 1)
    private void lunararc$reportChange(int slot, ItemStack expected, Supplier<ItemStack> actual, CallbackInfo ci) {
        ItemStack previous = this.lunararc$previousStack;
        this.lunararc$previousStack = null;
        if (previous == null || slot >= this.slots.size()) return;
        LunarArcGameplayEvents.inventorySlotChanged(this.slots.get(slot), slot, previous, this.lastSlots.get(slot));
    }
}
