package io.lunararcdevs.lunararc.common.mixin.core.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.lunararcdevs.lunararc.common.event.LunarArcMoreEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BeehiveBlockEntity.class)
public abstract class EntityEvents_HiveMixin {
    @Inject(method = "addOccupant", at = @At("HEAD"), cancellable = true, require = 0)
    private void lunararc$enter(Entity entity, CallbackInfo ci) {
        net.minecraft.world.level.block.entity.BlockEntity self = (net.minecraft.world.level.block.entity.BlockEntity) (Object) this;
        if (!LunarArcMoreEvents.enterBlock(entity, self.getLevel(), self.getBlockPos())) {
            if (entity instanceof net.minecraft.world.entity.animal.Bee bee) bee.setStayOutOfHiveCountdown(400);
            ci.cancel();
        }
    }
}
