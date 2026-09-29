package io.lunararcdevs.lunararc.common.mixin.core.entity;

import com.destroystokyo.paper.event.entity.CreeperIgniteEvent;
import io.lunararcdevs.lunararc.common.bridge.entity.CreeperBridge;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Creeper.class)
public abstract class CreeperMixin implements CreeperBridge {
    @Shadow private int swell;
    @Shadow private int maxSwell;
    @Shadow private int explosionRadius;
    @Shadow @Final private static EntityDataAccessor<Boolean> DATA_IS_POWERED;
    @Shadow @Final private static EntityDataAccessor<Boolean> DATA_IS_IGNITED;
    @Shadow public abstract boolean isIgnited();
    @Shadow private void explodeCreeper() { throw new AssertionError(); }

    @Unique private Entity lunararc$igniter;
    @Unique private boolean lunararc$directIgnite;

    @Override public int lunararc$getSwell() { return this.swell; }
    @Override public void lunararc$setSwell(int ticks) { this.swell = ticks; }
    @Override public int lunararc$getMaxSwell() { return this.maxSwell; }
    @Override public void lunararc$setMaxSwell(int ticks) { this.maxSwell = ticks; }
    @Override public int lunararc$getExplosionRadius() { return this.explosionRadius; }
    @Override public void lunararc$setExplosionRadius(int radius) { this.explosionRadius = radius; }
    @Override public void lunararc$setPowered(boolean powered) { ((Creeper) (Object) this).getEntityData().set(DATA_IS_POWERED, powered); }
    @Override public void lunararc$setIgnitedDirect(boolean ignited) {
        this.lunararc$directIgnite = true;
        try { ((Creeper) (Object) this).getEntityData().set(DATA_IS_IGNITED, ignited); }
        finally { this.lunararc$directIgnite = false; }
    }
    @Override public void lunararc$explode() { this.explodeCreeper(); }
    @Override public Entity lunararc$getIgniter() { return this.lunararc$igniter; }
    @Override public void lunararc$setIgniter(Entity entity) { this.lunararc$igniter = entity; }

    @Inject(method = "ignite", at = @At("HEAD"), cancellable = true)
    private void lunararc$creeperIgniteEvent(CallbackInfo ci) {
        if (this.lunararc$directIgnite || this.isIgnited()) return;
        Creeper self = (Creeper) (Object) this;
        CreeperIgniteEvent event = new CreeperIgniteEvent((org.bukkit.entity.Creeper) ((io.lunararcdevs.lunararc.common.bridge.EntityBridge) self).lunararc$getBukkitEntity(), true);
        if (!event.callEvent() || !event.isIgnited()) ci.cancel();
    }

    @Unique private org.bukkit.event.entity.ExplosionPrimeEvent lunararc$primeEvent;

    @Inject(method = "explodeCreeper", cancellable = true, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;explode(Lnet/minecraft/world/entity/Entity;DDDFLnet/minecraft/world/level/Level$ExplosionInteraction;)Lnet/minecraft/world/level/Explosion;"))
    private void lunararc$explosionPrime(CallbackInfo ci) {
        Creeper self = (Creeper) (Object) this;
        float radius = this.explosionRadius * (self.isPowered() ? 2.0F : 1.0F);
        this.lunararc$primeEvent = org.bukkit.craftbukkit.event.CraftEventFactory.callExplosionPrimeEvent(self, radius, false);
        if (this.lunararc$primeEvent.isCancelled()) {
            this.lunararc$primeEvent = null;
            ((LivingEntityAccessor) self).lunararc$setDead(false);
            this.swell = 0;
            this.lunararc$setIgnitedDirect(false);
            ci.cancel();
        }
    }

    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "explodeCreeper", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;explode(Lnet/minecraft/world/entity/Entity;DDDFLnet/minecraft/world/level/Level$ExplosionInteraction;)Lnet/minecraft/world/level/Explosion;"))
    private net.minecraft.world.level.Explosion lunararc$primedExplode(net.minecraft.world.level.Level level, Entity source, double x, double y, double z,
            float radius, net.minecraft.world.level.Level.ExplosionInteraction interaction,
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<net.minecraft.world.level.Explosion> original) {
        org.bukkit.event.entity.ExplosionPrimeEvent event = this.lunararc$primeEvent;
        this.lunararc$primeEvent = null;
        if (event == null) return original.call(level, source, x, y, z, radius, interaction);
        if (!event.getFire()) return original.call(level, source, x, y, z, event.getRadius(), interaction);
        return level.explode(source, x, y, z, event.getRadius(), true, interaction);
    }

    @Inject(method = "mobInteract", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/Creeper;ignite()V"))
    private void lunararc$captureIgniter(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        this.lunararc$igniter = player;
    }
}
