package io.lunararcdevs.lunararc.common.mixin.core.world.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.lunararcdevs.lunararc.common.mod.util.LunarArcInventories;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DropperBlock;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DropperBlock.class)
public abstract class DropperBlockMixin {
    @WrapOperation(method = "dispenseFrom", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/entity/HopperBlockEntity;addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/Container;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/Direction;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack lunararc$moveItem(Container source, Container destination, ItemStack stack, Direction direction, Operation<ItemStack> original) {
        if (InventoryMoveItemEvent.getHandlerList().getRegisteredListeners().length == 0) return original.call(source, destination, stack, direction);
        Inventory sourceInventory = LunarArcInventories.ownerInventory(source);
        Inventory destinationInventory = LunarArcInventories.ownerInventory(destination);
        if (sourceInventory == null || destinationInventory == null) return original.call(source, destination, stack, direction);
        org.bukkit.inventory.ItemStack original0 = CraftItemStack.asBukkitCopy(stack);
        InventoryMoveItemEvent event = new InventoryMoveItemEvent(sourceInventory, original0.clone(), destinationInventory, true);
        org.bukkit.Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return stack;
        ItemStack remainder = original.call(source, destination, CraftItemStack.asNMSCopy(event.getItem()), direction);
        return !event.getItem().equals(original0) && remainder.isEmpty() ? stack : remainder;
    }
}
