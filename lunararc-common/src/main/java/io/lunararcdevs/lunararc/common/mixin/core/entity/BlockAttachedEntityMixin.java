package io.lunararcdevs.lunararc.common.mixin.core.entity;

import io.lunararcdevs.lunararc.common.bridge.EntityBridge;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.decoration.BlockAttachedEntity;
import net.minecraft.world.phys.Vec3;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingBreakEvent.RemoveCause;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockAttachedEntity.class)
public abstract class BlockAttachedEntityMixin {
    @Inject(method = "tick", cancellable = true,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/decoration/BlockAttachedEntity;discard()V"))
    private void lunararc$breakOnTick(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        RemoveCause cause = self.level().getBlockState(self.blockPosition()).isAir() ? RemoveCause.PHYSICS : RemoveCause.OBSTRUCTION;
        if (lunararc$breakCancelled(cause, null)) ci.cancel();
    }

    @Inject(method = "hurt", cancellable = true,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/decoration/BlockAttachedEntity;kill()V"))
    private void lunararc$breakOnHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        Entity damager = !source.isDirect() && source.getEntity() != null ? source.getEntity() : source.getDirectEntity();
        boolean explosion = source.is(DamageTypeTags.IS_EXPLOSION);
        RemoveCause cause = explosion ? RemoveCause.EXPLOSION : damager != null ? RemoveCause.ENTITY : RemoveCause.DEFAULT;
        if (lunararc$breakCancelled(cause, damager)) cir.setReturnValue(true);
    }

    @Inject(method = "move", cancellable = true,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/decoration/BlockAttachedEntity;kill()V"))
    private void lunararc$breakOnMove(MoverType type, Vec3 movement, CallbackInfo ci) {
        if (lunararc$breakCancelled(RemoveCause.PHYSICS, null)) ci.cancel();
    }

    @Unique
    private boolean lunararc$breakCancelled(RemoveCause cause, Entity damager) {
        if (!(((EntityBridge) this).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Hanging hanging)) return false;
        HangingBreakEvent event = damager != null
                ? new HangingBreakByEntityEvent(hanging, ((EntityBridge) damager).lunararc$getBukkitEntity(), cause)
                : new HangingBreakEvent(hanging, cause);
        org.bukkit.Bukkit.getPluginManager().callEvent(event);
        return ((Entity) (Object) this).isRemoved() || event.isCancelled();
    }
}
