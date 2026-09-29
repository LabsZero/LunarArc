package io.lunararcdevs.lunararc.common.mixin.core.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.lunararcdevs.lunararc.common.bridge.EntityBridge;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Illusioner;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.Level;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({AbstractSkeleton.class, Illusioner.class})
public abstract class MobShootBowMixin {
    @WrapOperation(method = "performRangedAttack", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean lunararc$shootBow(Level level, Entity projectile, Operation<Boolean> original) {
        if (!(projectile instanceof AbstractArrow arrow) || level.isClientSide || !org.bukkit.Bukkit.isPrimaryThread()) {
            return original.call(level, projectile);
        }
        LivingEntity self = (LivingEntity) (Object) this;
        var event = CraftEventFactory.callEntityShootBowEvent(self, self.getMainHandItem(), arrow.getPickupItemStackOrigin(),
                arrow, InteractionHand.MAIN_HAND, 0.8F, true);
        if (event.isCancelled()) {
            event.getProjectile().remove();
            return false;
        }
        if (event.getProjectile() != ((EntityBridge) arrow).lunararc$getBukkitEntity()) {
            return true;
        }
        return original.call(level, projectile);
    }
}
