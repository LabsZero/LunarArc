package io.ampznetwork.lunararc.common.mixin.core.world;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.ampznetwork.lunararc.common.bridge.EntityBridge;
import io.ampznetwork.lunararc.common.mod.server.LunarArcTickingTrackerImpl;
import io.ampznetwork.lunararc.common.mod.util.LunarArcInventories;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.inventory.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fires {@link InventoryMoveItemEvent} and {@link InventoryPickupItemEvent}.
 *
 * <p>These are how a plugin sees items move on their own. Chest-shop plugins use them to stop a
 * hopper draining a shop's stock, protection plugins to stop one reaching into a claim, and
 * anti-lag plugins to cap transfers. None of it worked here, because neither event was fired.</p>
 *
 * <p>Both are guarded on having a registered listener before anything is built. That is Paper's own
 * optimisation and it matters more here than upstream: a hopper runs every tick it is not on
 * cooldown, on every hopper on the server, so resolving two inventories and copying an ItemStack
 * per transfer would be a permanent cost paid by servers that never listen. It also keeps this
 * whole path inert - no event, no cancellation, no behaviour change of any kind - on a server with
 * no plugin interested in it.</p>
 *
 * <p>The cancel path returns the input stack unchanged. {@code addItem} returns what is left over,
 * so handing back everything says nothing moved, and vanilla's own "did the transfer succeed" test
 * then reverts exactly as it would for a full destination. Arclight takes the same route for the
 * same reason; CraftBukkit instead restores the slot by hand, which reaches into state this has no
 * business touching from outside the method.</p>
 *
 * <p>{@code addItem} is static and does not know which hopper is asking, so the cooldown that
 * follows a cancel comes from the block entity currently ticking - the same technique Arclight uses
 * through ArclightCaptures. Eight ticks is vanilla's own transfer cooldown and Spigot's default
 * hopper-transfer; without it a cancelled transfer is retried on the very next tick, so a plugin
 * saying no to one hopper would have it asked again twenty times a second.</p>
 */
@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin {

    @Inject(
            method = "addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/entity/item/ItemEntity;)Z",
            at = @At("HEAD"),
            cancellable = true,
            require = 0)
    private static void lunararc$hopperPickupItem(Container container, ItemEntity itemEntity,
            CallbackInfoReturnable<Boolean> cir) {
        if (InventoryPickupItemEvent.getHandlerList().getRegisteredListeners().length == 0) return;
        Inventory inventory = LunarArcInventories.ownerInventory(container);
        if (inventory == null) return;
        if (!(((EntityBridge) itemEntity).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Item item)) {
            return;
        }

        InventoryPickupItemEvent event = new InventoryPickupItemEvent(inventory, item);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) cir.setReturnValue(false);
    }

    @WrapOperation(
            method = "ejectItems",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/HopperBlockEntity;addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/Container;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/Direction;)Lnet/minecraft/world/item/ItemStack;"),
            require = 0)
    private static ItemStack lunararc$hopperPush(Container source, Container destination, ItemStack stack,
            Direction direction, Operation<ItemStack> original) {
        ItemStack decided = lunararc$moveItem(source, destination, stack, true);
        if (decided == null) return stack;
        return original.call(source, destination, decided, direction);
    }

    @WrapOperation(
            method = "tryTakeInItemFromSlot",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/entity/HopperBlockEntity;addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/Container;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/Direction;)Lnet/minecraft/world/item/ItemStack;"),
            require = 0)
    private static ItemStack lunararc$hopperPull(Container source, Container destination, ItemStack stack,
            Direction direction, Operation<ItemStack> original) {
        ItemStack decided = lunararc$moveItem(source, destination, stack, false);
        if (decided == null) return stack;
        return original.call(source, destination, decided, direction);
    }

    /**
     * The stack to move on, or null if a plugin cancelled the transfer.
     *
     * <p>{@code sourceInitiated} distinguishes a hopper pushing into a container from a hopper
     * pulling out of one; the hopper is the source in the first case and the destination in the
     * second.</p>
     *
     * <p>The item goes to the event as a live mirror, not a copy, which is deliberate and is
     * Paper's choice at the same point. A plugin that adjusts the stack in place - the common
     * shape, {@code event.getItem().setAmount(n)} - is then adjusting the stack that actually
     * moves. Hand it a copy and that edit is silently dropped.</p>
     *
     * <p>What comes back is the caller's own object unless the plugin replaced it outright. That
     * matters: vanilla passes a stack it still holds a reference to, and {@code addItem} shrinks
     * the stack it is given as it transfers. Substituting a fresh copy for every transfer would
     * leave vanilla's own bookkeeping looking at an object that never shrank, which is how a
     * hopper ends up both keeping an item and delivering it. Paper guards this with a flag
     * recording whether setItem was called; the identity check here answers the same question
     * without needing Paper's own event subclass to ask it.</p>
     */
    @Unique
    private static ItemStack lunararc$moveItem(Container source, Container destination, ItemStack stack,
            boolean sourceInitiated) {
        if (InventoryMoveItemEvent.getHandlerList().getRegisteredListeners().length == 0) return stack;

        Inventory sourceInventory = LunarArcInventories.ownerInventory(source);
        Inventory destinationInventory = LunarArcInventories.ownerInventory(destination);
        if (sourceInventory == null || destinationInventory == null) return stack;

        org.bukkit.inventory.ItemStack mirror = CraftItemStack.asCraftMirror(stack);
        InventoryMoveItemEvent event =
                new InventoryMoveItemEvent(sourceInventory, mirror, destinationInventory, sourceInitiated);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            lunararc$delayTickingHopper();
            return null;
        }
        // Same object back means no plugin called setItem, so vanilla keeps the stack it passed -
        // including any in-place edit, which the mirror already applied to it.
        return event.getItem() == mirror ? stack : CraftItemStack.asNMSCopy(event.getItem());
    }

    @Unique
    private static void lunararc$delayTickingHopper() {
        if (LunarArcTickingTrackerImpl.INSTANCE.getTickingSource() instanceof HopperBlockEntity hopper) {
            // 8 ticks: vanilla's own transfer cooldown, and Spigot's default hopper-transfer.
            hopper.setCooldown(8);
        }
    }
}
