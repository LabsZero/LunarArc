package io.lunararcdevs.lunararc.common.mixin.core.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WitherBoss.class)
public abstract class WitherBossMixin {
    @Unique private boolean lunararc$canPortal = false;

    public void setCanTravelThroughPortals(boolean canPortal) {
        this.lunararc$canPortal = canPortal;
    }

    @Inject(method = "canUsePortal", at = @At("HEAD"), cancellable = true)
    private void lunararc$portal(boolean allowPassengers, CallbackInfoReturnable<Boolean> cir) {
        if (this.lunararc$canPortal) cir.setReturnValue(true);
    }

    @WrapOperation(method = "customServerAiStep", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;)Z"))
    private boolean lunararc$witherBreakBlock(Level level, BlockPos pos, boolean drop, Entity breaker, Operation<Boolean> original) {
        if (!CraftEventFactory.callEntityChangeBlockEvent((WitherBoss) (Object) this, pos,
                net.minecraft.world.level.block.Blocks.AIR.defaultBlockState())) {
            return false;
        }
        return original.call(level, pos, drop, breaker);
    }

    @WrapOperation(method = "customServerAiStep", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;explode(Lnet/minecraft/world/entity/Entity;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;)Lnet/minecraft/world/level/Explosion;"))
    private Explosion lunararc$witherExplode(Level level, Entity source, double x, double y, double z, float radius, boolean fire,
            Level.ExplosionInteraction interaction, Operation<Explosion> original) {
        ExplosionPrimeEvent event = CraftEventFactory.callExplosionPrimeEvent(source, radius, fire);
        if (event.isCancelled()) {
            return null;
        }
        return original.call(level, source, x, y, z, event.getRadius(), event.getFire(), interaction);
    }
}
