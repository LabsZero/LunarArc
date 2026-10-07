package io.lunararcdevs.lunararc.common.mixin.core.entity;

import io.lunararcdevs.lunararc.common.LunarArcServerAccess;
import io.lunararcdevs.lunararc.common.bridge.EntityBridge;
import io.lunararcdevs.lunararc.common.bridge.PlayerAffectsSpawningBridge;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(Player.class)
public abstract class PlayerMixin implements PlayerAffectsSpawningBridge, io.lunararcdevs.lunararc.common.bridge.PlayerExhaustionBridge {
    public org.bukkit.craftbukkit.entity.CraftHumanEntity getBukkitEntity() {
        return (org.bukkit.craftbukkit.entity.CraftHumanEntity) ((EntityBridge) (Object) this).lunararc$getBukkitEntity();
    }

    @Inject(method = "attack", at = @At("HEAD"), cancellable = true, require = 0)
    private void lunararc$preAttackEvent(net.minecraft.world.entity.Entity target, CallbackInfo ci) {
        if (io.lunararcdevs.lunararc.common.event.LunarArcPaperEvents.preAttack((Player) (Object) this, target)) ci.cancel();
    }

    @Inject(method = "startAutoSpinAttack", at = @At("HEAD"), require = 0)
    private void lunararc$riptideEvent(int ticks, float damage, ItemStack stack, CallbackInfo ci) {
        io.lunararcdevs.lunararc.common.event.LunarArcPaperEvents.riptide((Player) (Object) this, stack);
    }

    @Unique private net.minecraft.world.entity.LivingEntity lunararc$shieldAttacker;

    @Inject(method = "blockUsingShield", at = @At("HEAD"), require = 0)
    private void lunararc$shieldAttacker(net.minecraft.world.entity.LivingEntity attacker, CallbackInfo ci) {
        this.lunararc$shieldAttacker = attacker;
    }

    @Inject(method = "blockUsingShield", at = @At("RETURN"), require = 0)
    private void lunararc$clearShieldAttacker(net.minecraft.world.entity.LivingEntity attacker, CallbackInfo ci) {
        this.lunararc$shieldAttacker = null;
    }

    @Inject(method = "disableShield", at = @At("HEAD"), cancellable = true, require = 0)
    private void lunararc$disableShield(CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (this.lunararc$shieldAttacker == null) return;
        int cooldown = io.lunararcdevs.lunararc.common.event.LunarArcPaperEvents.shieldDisable(self, this.lunararc$shieldAttacker);
        if (cooldown == 100) return;
        ci.cancel();
        if (cooldown < 0) return;
        self.getCooldowns().addCooldown(self.getUseItem().getItem(), cooldown);
        self.stopUsingItem();
        self.level().broadcastEntityEvent(self, (byte) 30);
    }

    @Unique
    private boolean lunararc$affectsSpawning = true;
    @Unique private org.bukkit.event.entity.EntityExhaustionEvent.ExhaustionReason lunararc$exhaustionReason = org.bukkit.event.entity.EntityExhaustionEvent.ExhaustionReason.UNKNOWN;

    @Override
    public boolean lunararc$getAffectsSpawning() {
        return this.lunararc$affectsSpawning;
    }

    @Override
    public void lunararc$setAffectsSpawning(boolean affectsSpawning) {
        this.lunararc$affectsSpawning = affectsSpawning;
    }

    @Override
    public void lunararc$pushExhaustionReason(org.bukkit.event.entity.EntityExhaustionEvent.ExhaustionReason reason) {
        this.lunararc$exhaustionReason = java.util.Objects.requireNonNull(reason, "reason");
    }


    @Inject(
            method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;",
            at = @At("RETURN"),
            cancellable = true,
            require = 0)
    private void lunararc$onDrop(
            ItemStack droppedStack,
            boolean dropAround,
            boolean traceItem,
            CallbackInfoReturnable<ItemEntity> cir) {
        ItemEntity dropped = cir.getReturnValue();
        if (dropped == null || droppedStack.isEmpty()) {
            return;
        }

        Player handle = (Player) (Object) this;
        if (!(handle instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return;
        }
        Object bukkitPlayerObject = ((EntityBridge) handle).lunararc$getBukkitEntity();
        Object bukkitItemObject = ((EntityBridge) dropped).lunararc$getBukkitEntity();
        if (!(bukkitPlayerObject instanceof org.bukkit.entity.Player bukkitPlayer)
                || !(bukkitItemObject instanceof org.bukkit.entity.Item bukkitItem)) {
            return;
        }

        org.bukkit.event.player.PlayerDropItemEvent event =
                new org.bukkit.event.player.PlayerDropItemEvent(bukkitPlayer, bukkitItem);
        LunarArcServerAccess.getCraftServer(serverPlayer.server)
                .getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            return;
        }
        
        dropped.discard();

        org.bukkit.inventory.ItemStack restore = bukkitItem.getItemStack();
        if (traceItem) {
            org.bukkit.inventory.ItemStack current = bukkitPlayer.getInventory().getItemInMainHand();
            if (current.getType().isAir()) {
                bukkitPlayer.getInventory().setItemInMainHand(restore);
            } else if (current.isSimilar(restore)
                    && current.getAmount() < current.getMaxStackSize()
                    && restore.getAmount() == 1) {
                current.setAmount(current.getAmount() + 1);
                bukkitPlayer.getInventory().setItemInMainHand(current);
            } else {
                bukkitPlayer.getInventory().addItem(restore);
            }
        } else {
            bukkitPlayer.getInventory().addItem(restore);
        }
        cir.setReturnValue(null);
    }
    @com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(method = "causeFoodExhaustion")
    private void lunararc$exhaustion(float exhaustion, com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original) {
        Player handle = (Player) (Object) this;
        var reason = this.lunararc$exhaustionReason;
        this.lunararc$exhaustionReason = org.bukkit.event.entity.EntityExhaustionEvent.ExhaustionReason.UNKNOWN;
        Object bukkit = ((EntityBridge) handle).lunararc$getBukkitEntity();
        if (!(bukkit instanceof org.bukkit.entity.HumanEntity human) || handle.level().isClientSide) {
            original.call(exhaustion);
            return;
        }
        var event = new org.bukkit.event.entity.EntityExhaustionEvent(human, reason, exhaustion);
        org.bukkit.Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) original.call(event.getExhaustion());
    }

    @Inject(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;heal(F)V"), require = 0)
    private void lunararc$peacefulRegen(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        ((io.lunararcdevs.lunararc.common.bridge.LivingEntityBridge) this)
                .lunararc$pushHealReason(org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason.REGEN, false);
    }


    @Inject(
            method = "jumpFromGround",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;causeFoodExhaustion(F)V"),
            require = 0)
    private void lunararc$jumpExhaustion(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        Player self = (Player) (Object) this;
        this.lunararc$pushExhaustionReason(self.isSprinting()
                ? org.bukkit.event.entity.EntityExhaustionEvent.ExhaustionReason.JUMP_SPRINT
                : org.bukkit.event.entity.EntityExhaustionEvent.ExhaustionReason.JUMP);
    }

    @Inject(
            method = "actuallyHurt",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;causeFoodExhaustion(F)V"),
            require = 0)
    private void lunararc$damageExhaustion(
            net.minecraft.world.damagesource.DamageSource source, float amount,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        this.lunararc$pushExhaustionReason(org.bukkit.event.entity.EntityExhaustionEvent.ExhaustionReason.DAMAGED);
    }

    @Inject(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;causeFoodExhaustion(F)V"), require = 0)
    private void lunararc$attackExhaustion(net.minecraft.world.entity.Entity target, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        this.lunararc$pushExhaustionReason(org.bukkit.event.entity.EntityExhaustionEvent.ExhaustionReason.ATTACK);
    }

    @Inject(
            method = "turtleHelmetTick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z"),
            require = 0)
    private void lunararc$turtleHelmetCause(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        ((io.lunararcdevs.lunararc.common.bridge.LivingEntityBridge) this)
                .lunararc$pushEffectCause(org.bukkit.event.entity.EntityPotionEffectEvent.Cause.TURTLE_HELMET);
    }


    @WrapOperation(method = "tryToStartFallFlying", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;startFallFlying()V"))
    private void lunararc$startGlide(Player self, Operation<Void> original) {
        if (!org.bukkit.craftbukkit.event.CraftEventFactory.callToggleGlideEvent(self, true).isCancelled()) {
            original.call(self);
        }
    }
}
