package io.lunararcdevs.lunararc.common.mixin.core.world.dispenser;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DefaultDispenseItemBehavior.class)
public abstract class DefaultDispenseItemBehaviorMixin {
    @Unique private static BlockSource lunararc$source;
    @Unique private static boolean lunararc$cancelled;

    @Inject(method = "execute", at = @At("HEAD"))
    private void lunararc$captureSource(BlockSource source, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        lunararc$source = org.bukkit.Bukkit.isPrimaryThread() && !CraftEventFactory.dispenseEventFired ? source : null;
        lunararc$cancelled = false;
    }

    @WrapOperation(method = "spawnItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private static boolean lunararc$dispense(Level level, Entity entity, Operation<Boolean> original) {
        BlockSource source = lunararc$source;
        lunararc$source = null;
        if (source == null || !(entity instanceof ItemEntity item)) return original.call(level, entity);
        var event = CraftEventFactory.callBlockDispenseEvent(source, item.getItem(), item.getDeltaMovement());
        if (event.isCancelled()) {
            lunararc$cancelled = true;
            return false;
        }
        item.setItem(CraftItemStack.asNMSCopy(event.getItem()));
        item.setDeltaMovement(event.getVelocity().getX(), event.getVelocity().getY(), event.getVelocity().getZ());
        return original.call(level, entity);
    }

    @Inject(method = "execute", at = @At("RETURN"))
    private void lunararc$restoreCancelled(BlockSource source, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        if (lunararc$cancelled) {
            lunararc$cancelled = false;
            cir.getReturnValue().grow(1);
        }
        lunararc$source = null;
    }
}
