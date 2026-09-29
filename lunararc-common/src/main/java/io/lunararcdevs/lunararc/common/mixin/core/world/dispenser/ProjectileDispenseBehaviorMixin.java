package io.lunararcdevs.lunararc.common.mixin.core.world.dispenser;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.ProjectileDispenseBehavior;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ProjectileDispenseBehavior.class)
public abstract class ProjectileDispenseBehaviorMixin {
    @Inject(method = "execute", cancellable = true, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private void lunararc$dispense(BlockSource source, ItemStack stack, CallbackInfoReturnable<ItemStack> cir, @Local Projectile projectile) {
        if (!org.bukkit.Bukkit.isPrimaryThread() || CraftEventFactory.dispenseEventFired) return;
        var event = CraftEventFactory.callBlockDispenseEvent(source, stack.copyWithCount(1), projectile.getDeltaMovement());
        if (event.isCancelled()) {
            cir.setReturnValue(stack);
            return;
        }
        projectile.setDeltaMovement(event.getVelocity().getX(), event.getVelocity().getY(), event.getVelocity().getZ());
    }
}
