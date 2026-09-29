package io.lunararcdevs.lunararc.common.mixin.core.world.dispenser;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.core.dispenser.ProjectileDispenseBehavior;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(net.minecraft.world.level.block.DispenserBlock.class)
public abstract class DispenserBlockMixin {
    @Shadow protected abstract DispenseItemBehavior getDispenseMethod(net.minecraft.world.level.Level level, ItemStack stack);

    @WrapOperation(method = "dispenseFrom", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/core/dispenser/DispenseItemBehavior;dispense(Lnet/minecraft/core/dispenser/BlockSource;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack lunararc$dispense(DispenseItemBehavior behavior, BlockSource source, ItemStack stack, Operation<ItemStack> original) {
        if (behavior.getClass() == DefaultDispenseItemBehavior.class || behavior instanceof ProjectileDispenseBehavior
                || !org.bukkit.Bukkit.isPrimaryThread()) {
            return original.call(behavior, source, stack);
        }
        ItemStack single = stack.copyWithCount(1);
        var event = CraftEventFactory.callBlockDispenseEvent(source, single, Vec3.ZERO);
        if (event.isCancelled()) return stack;
        ItemStack eventItem = CraftItemStack.asNMSCopy(event.getItem());
        CraftEventFactory.dispenseEventFired = true;
        try {
            if (!ItemStack.isSameItemSameComponents(eventItem, single)) {
                DispenseItemBehavior replacement = this.getDispenseMethod(source.level(), eventItem);
                if (replacement != DispenseItemBehavior.NOOP && replacement != behavior) {
                    replacement.dispense(source, eventItem);
                    return stack;
                }
            }
            return original.call(behavior, source, stack);
        } finally {
            CraftEventFactory.dispenseEventFired = false;
        }
    }
}
