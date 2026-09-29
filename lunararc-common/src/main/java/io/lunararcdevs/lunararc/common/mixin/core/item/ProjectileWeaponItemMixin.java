package io.lunararcdevs.lunararc.common.mixin.core.item;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.lunararcdevs.lunararc.common.bridge.EntityBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ProjectileWeaponItem.class)
public abstract class ProjectileWeaponItemMixin {
    @Unique private boolean lunararc$projectileReplaced;

    @Inject(method = "shoot", cancellable = true, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private void lunararc$shootBow(ServerLevel level, LivingEntity shooter, InteractionHand hand, ItemStack weapon, List<ItemStack> projectileItems,
            float velocity, float inaccuracy, boolean isCrit, LivingEntity target, CallbackInfo ci,
            @Local(ordinal = 1) ItemStack ammo, @Local Projectile projectile) {
        this.lunararc$projectileReplaced = false;
        if (!org.bukkit.Bukkit.isPrimaryThread()) return;
        var event = CraftEventFactory.callEntityShootBowEvent(shooter, weapon, ammo, projectile, hand, velocity, true);
        if (event.isCancelled()) {
            event.getProjectile().remove();
            ci.cancel();
            return;
        }
        this.lunararc$projectileReplaced = event.getProjectile() != ((EntityBridge) projectile).lunararc$getBukkitEntity();
    }

    @WrapOperation(method = "shoot", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean lunararc$addShotProjectile(ServerLevel level, Entity projectile, Operation<Boolean> original) {
        if (this.lunararc$projectileReplaced) {
            this.lunararc$projectileReplaced = false;
            return true;
        }
        return original.call(level, projectile);
    }
}
