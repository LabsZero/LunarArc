package io.lunararcdevs.lunararc.common.mixin.core.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.lunararcdevs.lunararc.common.event.LunarArcMoreEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ThrownPotion.class)
public abstract class EntityEvents_PotionMixin {
    @WrapOperation(method = "makeAreaOfEffectCloud", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean lunararc$lingering(Level level, Entity cloud, Operation<Boolean> original) {
        if (!LunarArcMoreEvents.lingering((ThrownPotion) (Object) this, cloud)) {
            cloud.discard();
            return false;
        }
        return original.call(level, cloud);
    }
}
