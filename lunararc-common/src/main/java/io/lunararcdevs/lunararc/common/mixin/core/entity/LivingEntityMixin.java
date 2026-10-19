package io.lunararcdevs.lunararc.common.mixin.core.entity;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.lunararcdevs.lunararc.common.bridge.LivingEntityBridge;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.util.TriState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.entity.EntityDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements LivingEntityBridge {
    /** Paper compatibility: Paper classes (for example the combat tracker wrapper) call this patched accessor. */
    public org.bukkit.craftbukkit.entity.CraftLivingEntity getBukkitLivingEntity() {
        return (org.bukkit.craftbukkit.entity.CraftLivingEntity) ((io.lunararcdevs.lunararc.common.bridge.EntityBridge) this).lunararc$getBukkitEntity();
    }

    @org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final private static net.minecraft.network.syncher.EntityDataAccessor<Integer> DATA_ARROW_COUNT_ID;
    @org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final private static int LIVING_ENTITY_FLAG_SPIN_ATTACK;
    @org.spongepowered.asm.mixin.Shadow private static byte entityEventForEquipmentBreak(net.minecraft.world.entity.EquipmentSlot slot) { throw new AssertionError(); }
    @org.spongepowered.asm.mixin.Shadow protected abstract void completeUsingItem();
    @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true, require = 0)
    private void lunararc$jumpEvent(CallbackInfo ci) {
        if (io.lunararcdevs.lunararc.common.event.LunarArcPaperEvents.jump((LivingEntity) (Object) this)) ci.cancel();
    }

    @Inject(method = "releaseUsingItem", at = @At("HEAD"), require = 0)
    private void lunararc$stopUsingEvent(CallbackInfo ci) {
        io.lunararcdevs.lunararc.common.event.LunarArcPaperEvents.stopUsing((LivingEntity) (Object) this);
    }

    @Inject(method = "checkTotemDeathProtection", at = @At("HEAD"), cancellable = true, require = 0)
    private void lunararc$totemEvent(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        Boolean resurrected = io.lunararcdevs.lunararc.common.event.LunarArcPaperEvents.resurrect((LivingEntity) (Object) this, source);
        if (resurrected != null) cir.setReturnValue(resurrected);
    }

    @Unique private TriState lunararc$frictionState = TriState.NOT_SET;
    @Unique private int lunararc$shieldBlockingDelay = 5;
    @Unique private int lunararc$maximumAirOverride = -1;
    @Unique private boolean lunararc$collidable = true;
    @Unique private final Set<UUID> lunararc$collidableExemptions = new HashSet<>();
    @Unique private boolean lunararc$bukkitCanPickupItems;
    @Unique private org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason lunararc$healReason = org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason.CUSTOM;
    @Unique private boolean lunararc$fastRegen;
    @Unique private org.bukkit.event.entity.EntityPotionEffectEvent.Cause lunararc$effectCause = org.bukkit.event.entity.EntityPotionEffectEvent.Cause.UNKNOWN;
    @Unique private boolean lunararc$suppressEffectEvent;

    @Override public void lunararc$completeUsingItem() { this.completeUsingItem(); }

    @WrapOperation(method = "completeUsingItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;finishUsingItem(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;"))
    private net.minecraft.world.item.ItemStack lunararc$itemConsume(net.minecraft.world.item.ItemStack useItem,
            net.minecraft.world.level.Level level, LivingEntity entity, Operation<net.minecraft.world.item.ItemStack> original) {
        if (!(entity instanceof net.minecraft.server.level.ServerPlayer)
                || !(((io.lunararcdevs.lunararc.common.bridge.EntityBridge) entity).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Player player)) {
            return original.call(useItem, level, entity);
        }
        org.bukkit.inventory.ItemStack craftItem = org.bukkit.craftbukkit.inventory.CraftItemStack.asBukkitCopy(useItem);
        var event = new org.bukkit.event.player.PlayerItemConsumeEvent(player, craftItem,
                org.bukkit.craftbukkit.CraftEquipmentSlot.getHand(entity.getUsedItemHand()));
        org.bukkit.Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            player.updateInventory();
            player.sendHealthUpdate();
            return useItem;
        }
        net.minecraft.world.item.ItemStack result = event.getItem().equals(craftItem)
                ? original.call(useItem, level, entity)
                : original.call(org.bukkit.craftbukkit.inventory.CraftItemStack.asNMSCopy(event.getItem()), level, entity);
        if (result != null && event.getReplacement() != null) {
            result = org.bukkit.craftbukkit.inventory.CraftItemStack.asNMSCopy(event.getReplacement());
        }
        return result;
    }
    @Override public net.minecraft.network.syncher.EntityDataAccessor<Integer> lunararc$getArrowCountDataAccessorBridge() { return DATA_ARROW_COUNT_ID; }
    @Override public int lunararc$getSpinAttackFlagBridge() { return LIVING_ENTITY_FLAG_SPIN_ATTACK; }
    @Override public byte lunararc$entityEventForEquipmentBreakBridge(net.minecraft.world.entity.EquipmentSlot slot) { return entityEventForEquipmentBreak(slot); }
    @Override public TriState lunararc$getFrictionState() { return lunararc$frictionState; }
    @Override public void lunararc$setFrictionState(TriState state) { lunararc$frictionState = java.util.Objects.requireNonNull(state, "state"); }
    @Override public int lunararc$getShieldBlockingDelay() { return lunararc$shieldBlockingDelay; }
    @Override public void lunararc$setShieldBlockingDelay(int delay) {
        if (delay < 0) throw new IllegalArgumentException("Shield blocking delay must be >= 0");
        lunararc$shieldBlockingDelay = delay;
    }
    @Override public int lunararc$getMaximumAirOverride() { return lunararc$maximumAirOverride; }
    @Override public void lunararc$setMaximumAirOverride(int ticks) {
        if (ticks < 0) throw new IllegalArgumentException("Maximum air must be >= 0");
        lunararc$maximumAirOverride = ticks;
    }
    @Override public boolean lunararc$isCollidable() { return lunararc$collidable; }
    @Override public void lunararc$setCollidable(boolean collidable) { lunararc$collidable = collidable; }
    @Override public Set<UUID> lunararc$getCollidableExemptions() { return lunararc$collidableExemptions; }
    @Override public boolean lunararc$getBukkitCanPickupItems() { return lunararc$bukkitCanPickupItems; }
    @Override public void lunararc$setBukkitCanPickupItems(boolean pickup) { lunararc$bukkitCanPickupItems = pickup; }
    @Override public void lunararc$pushHealReason(org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason reason, boolean fastRegen) {
        this.lunararc$healReason = java.util.Objects.requireNonNull(reason, "reason");
        this.lunararc$fastRegen = fastRegen;
    }
    public boolean removeEffect(net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect,
            org.bukkit.event.entity.EntityPotionEffectEvent.Cause cause) {
        this.lunararc$effectCause = java.util.Objects.requireNonNull(cause, "cause");
        return ((LivingEntity) (Object) this).removeEffect(effect);
    }

    @Override public void lunararc$pushEffectCause(org.bukkit.event.entity.EntityPotionEffectEvent.Cause cause) {
        this.lunararc$effectCause = java.util.Objects.requireNonNull(cause, "cause");
    }

    @Override
    public boolean lunararc$removeAllEffects(org.bukkit.event.entity.EntityPotionEffectEvent.Cause cause) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity.level().isClientSide || entity.getActiveEffects().isEmpty()) {
            return false;
        }
        boolean changed = false;
        java.util.List<net.minecraft.world.effect.MobEffectInstance> effects =
                new java.util.ArrayList<>(entity.getActiveEffects());
        for (net.minecraft.world.effect.MobEffectInstance old : effects) {
            var event = CraftEventFactory.callEntityPotionEffectChangeEvent(
                    entity, old, null, cause,
                    org.bukkit.event.entity.EntityPotionEffectEvent.Action.CLEARED, true);
            if (event.isCancelled()) {
                continue;
            }
            this.lunararc$suppressEffectEvent = true;
            try {
                changed |= entity.removeEffect(old.getEffect());
            } finally {
                this.lunararc$suppressEffectEvent = false;
            }
        }
        return changed;
    }

    @Unique
    private org.bukkit.event.entity.EntityPotionEffectEvent.Cause lunararc$consumeEffectCause() {
        var cause = this.lunararc$effectCause;
        this.lunararc$effectCause = org.bukkit.event.entity.EntityPotionEffectEvent.Cause.UNKNOWN;
        return cause;
    }

    @WrapMethod(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z")
    private boolean lunararc$effectChange(
            net.minecraft.world.effect.MobEffectInstance incoming,
            net.minecraft.world.entity.Entity source,
            Operation<Boolean> original) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (this.lunararc$suppressEffectEvent || entity.level().isClientSide || !org.bukkit.Bukkit.isPrimaryThread()) {
            return original.call(incoming, source);
        }

        var cause = this.lunararc$consumeEffectCause();
        net.minecraft.world.effect.MobEffectInstance old = entity.getEffect(incoming.getEffect());
        if (old == null) {
            var event = CraftEventFactory.callEntityPotionEffectChangeEvent(entity, null, incoming, cause, true);
            return !event.isCancelled() && original.call(incoming, source);
        }

        net.minecraft.world.effect.MobEffectInstance probe = new net.minecraft.world.effect.MobEffectInstance(old);
        boolean vanillaOverride = probe.update(incoming);
        var event = CraftEventFactory.callEntityPotionEffectChangeEvent(entity, old, incoming, cause, vanillaOverride);
        if (event.isCancelled() || !event.isOverride()) {
            return false;
        }
        if (vanillaOverride) {
            return original.call(incoming, source);
        }

        this.lunararc$suppressEffectEvent = true;
        try {
            entity.removeEffect(incoming.getEffect());
        } finally {
            this.lunararc$suppressEffectEvent = false;
        }
        return original.call(incoming, source);
    }

    @WrapMethod(method = "removeEffect")
    private boolean lunararc$effectRemove(
            net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect,
            Operation<Boolean> original) {
        LivingEntity entity = (LivingEntity) (Object) this;
        // Same reasoning as lunararc$effectChange above — removeEffect() is a general vanilla
        // method reachable from worldgen/structure-population worker threads too.
        if (this.lunararc$suppressEffectEvent || entity.level().isClientSide || !org.bukkit.Bukkit.isPrimaryThread()) {
            return original.call(effect);
        }
        net.minecraft.world.effect.MobEffectInstance old = entity.getEffect(effect);
        if (old == null) {
            this.lunararc$consumeEffectCause();
            return original.call(effect);
        }
        var cause = this.lunararc$consumeEffectCause();
        var event = CraftEventFactory.callEntityPotionEffectChangeEvent(
                entity, old, null, cause, org.bukkit.event.entity.EntityPotionEffectEvent.Action.REMOVED, true);
        if (event.isCancelled()) {
            return false;
        }
        return original.call(effect);
    }

    @ModifyExpressionValue(
            method = "tickEffects",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/effect/MobEffectInstance;tickServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Ljava/lang/Runnable;)Z"),
            require = 1)
    private boolean lunararc$effectExpiration(
            boolean stillActive,
            @Local net.minecraft.world.effect.MobEffectInstance effect) {
        if (stillActive) {
            return true;
        }
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity.level().isClientSide) {
            return false;
        }
        var event = CraftEventFactory.callEntityPotionEffectChangeEvent(
                entity, effect, null, org.bukkit.event.entity.EntityPotionEffectEvent.Cause.EXPIRATION,
                org.bukkit.event.entity.EntityPotionEffectEvent.Action.REMOVED, true);

        return event.isCancelled();
    }

    /** The totem's consume effects (clear effects, regeneration...) run through DeathProtection#applyEffects. */
    @WrapOperation(method = "checkTotemDeathProtection", require = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/component/DeathProtection;applyEffects(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)V"))
    private void lunararc$totemEffects(net.minecraft.world.item.component.DeathProtection protection, net.minecraft.world.item.ItemStack stack,
            LivingEntity entity, Operation<Void> original) {
        io.lunararcdevs.lunararc.common.util.LunarArcEffectContext.totem(true);
        try {
            original.call(protection, stack, entity);
        } finally {
            io.lunararcdevs.lunararc.common.util.LunarArcEffectContext.totem(false);
        }
    }

    @WrapOperation(
            method = "checkPositionAndTeleport",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;teleportTo(DDD)V"),
            require = 0)
    private void lunararc$entityRandomTeleport(
            LivingEntity entity, double x, double y, double z, Operation<Void> original) {
        if (entity instanceof net.minecraft.server.level.ServerPlayer || entity.level().isClientSide) {
            original.call(entity, x, y, z);
            return;
        }
        Object bukkit = ((io.lunararcdevs.lunararc.common.bridge.EntityBridge) entity).lunararc$getBukkitEntity();
        if (!(bukkit instanceof org.bukkit.entity.Entity bukkitEntity)) {
            original.call(entity, x, y, z);
            return;
        }
        org.bukkit.Location from = bukkitEntity.getLocation();
        org.bukkit.Location to = new org.bukkit.Location(from.getWorld(), x, y, z, from.getYaw(), from.getPitch());
        var event = new org.bukkit.event.entity.EntityTeleportEvent(bukkitEntity, from, to);
        org.bukkit.Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled() || event.getTo() == null || event.getTo().getWorld() != from.getWorld()) {
            return;
        }
        org.bukkit.Location destination = event.getTo();
        original.call(entity, destination.getX(), destination.getY(), destination.getZ());
    }

    @WrapMethod(method = "heal")
    private void lunararc$regainHealth(float amount, Operation<Void> original) {
        LivingEntity entity = (LivingEntity) (Object) this;
        var reason = this.lunararc$healReason;
        boolean fast = this.lunararc$fastRegen;
        this.lunararc$healReason = org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason.CUSTOM;
        this.lunararc$fastRegen = false;
        Object bukkit = ((io.lunararcdevs.lunararc.common.bridge.EntityBridge) entity).lunararc$getBukkitEntity();
        if (!(bukkit instanceof org.bukkit.entity.LivingEntity living) || entity.level().isClientSide
                || !org.bukkit.Bukkit.isPrimaryThread()) {
            original.call(amount);
            return;
        }
        org.bukkit.event.entity.EntityRegainHealthEvent event =
                new org.bukkit.event.entity.EntityRegainHealthEvent(living, amount, reason, fast);
        org.bukkit.Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) original.call((float) event.getAmount());
    }

    @Inject(method = "shouldDiscardFriction", at = @At("RETURN"), cancellable = true, require = 0)
    private void lunararc$paperFriction(CallbackInfoReturnable<Boolean> cir) {
        if (lunararc$frictionState != TriState.NOT_SET) {
            cir.setReturnValue(lunararc$frictionState == TriState.FALSE);
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"), require = 1)
    private void lunararc$saveLivingState(net.minecraft.world.level.storage.ValueOutput output, CallbackInfo ci) {
        if (lunararc$frictionState != TriState.NOT_SET) {
            output.putString("Paper.FrictionState", lunararc$frictionState.toString());
        }
        if (lunararc$maximumAirOverride >= 0) {
            output.putInt("Bukkit.MaxAirSupply", lunararc$maximumAirOverride);
        }
        if (!lunararc$collidable) {
            output.putBoolean("Bukkit.Collidable", false);
        }
        if (lunararc$bukkitCanPickupItems) {
            output.putBoolean("Bukkit.PickupLoot", true);
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"), require = 1)
    private void lunararc$loadLivingState(net.minecraft.world.level.storage.ValueInput input, CallbackInfo ci) {
        lunararc$frictionState = TriState.NOT_SET;
        input.getString("Paper.FrictionState").ifPresent(name -> {
            try {
                lunararc$frictionState = TriState.valueOf(name);
            } catch (IllegalArgumentException ignored) {
                lunararc$frictionState = TriState.NOT_SET;
            }
        });
        lunararc$maximumAirOverride = input.getInt("Bukkit.MaxAirSupply").map(v -> Math.max(0, v)).orElse(-1);
        lunararc$collidable = input.getBooleanOr("Bukkit.Collidable", true);
        lunararc$bukkitCanPickupItems = input.getBooleanOr("Bukkit.PickupLoot", false);
    }

    /** Paper's ArmorStand#canTick: ArmorStand no longer overrides tick(), so gate it from the shared LivingEntity#tick. */
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, require = 1)
    private void lunararc$armorStandTickGate(CallbackInfo ci) {
        if (this instanceof io.lunararcdevs.lunararc.common.bridge.entity.ArmorStandTickBridge gate && !gate.lunararc$shouldTick()) ci.cancel();
    }

    @Unique private int lunararc$invulnerableDuration = 20;

    @Override public int lunararc$getInvulnerableDuration() { return this.lunararc$invulnerableDuration; }
    @Override public void lunararc$setInvulnerableDuration(int ticks) { this.lunararc$invulnerableDuration = ticks; }

    /** CraftBukkit's maximumNoDamageTicks: the half-way "recently hurt" threshold... */
    @org.spongepowered.asm.mixin.injection.ModifyConstant(method = "hurtServer", constant = @org.spongepowered.asm.mixin.injection.Constant(floatValue = 10.0F, ordinal = 0))
    private float lunararc$halfInvulnerableDuration(float vanilla) {
        return (float) this.lunararc$invulnerableDuration / 2.0F;
    }

    /** ...and the invulnerability a successful hit grants. */
    // NeoForge replaces this constant with DamageContainer#getPostAttackInvulnerabilityTicks (handled in its own mixin), hence optional.
    @org.spongepowered.asm.mixin.injection.ModifyConstant(method = "hurtServer", constant = @org.spongepowered.asm.mixin.injection.Constant(intValue = 20), require = 0)
    private int lunararc$invulnerableDuration(int vanilla) {
        return this.lunararc$invulnerableDuration;
    }

    @WrapMethod(method = "hurtServer")
    private boolean lunararc$onDamage(net.minecraft.server.level.ServerLevel level, DamageSource source, float amount, Operation<Boolean> original) {
        LivingEntity entity = (LivingEntity) (Object) this;

        if (entity.isDeadOrDying()
                || entity.isInvulnerableTo(level, source)
                || (source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)
                    && entity.hasEffect(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE))) {
            return original.call(level, source, amount);
        }

        EntityDamageEvent event = CraftEventFactory.callEntityDamageEvent(entity, source, amount);
        if (event.isCancelled()) return false;

        double adjusted = event.getFinalDamage();
        if (!Double.isFinite(adjusted)) adjusted = 0.0D;
        return original.call(level, source, (float) Math.max(0.0D, adjusted));
    }

    @org.spongepowered.asm.mixin.Shadow protected boolean dead;

    @WrapMethod(method = "die")
    private void lunararc$onDeath(DamageSource source, Operation<Void> original) {
        LivingEntity entity = (LivingEntity) (Object) this;
        original.call(source);
        if (lunararc$deathCancelled) {
            lunararc$deathCancelled = false;
            entity.setPose(net.minecraft.world.entity.Pose.STANDING);
        }
    }

    @Unique private boolean lunararc$deathCancelled;

    @WrapMethod(method = "dropAllDeathLoot")
    private void lunararc$dropAllDeathLoot(net.minecraft.server.level.ServerLevel level, DamageSource source, Operation<Void> original) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity instanceof net.minecraft.server.level.ServerPlayer || !org.bukkit.Bukkit.isPrimaryThread()) {
            original.call(level, source);
            return;
        }
        io.lunararcdevs.lunararc.common.event.LunarArcDeathCapture.Capture previous = io.lunararcdevs.lunararc.common.event.LunarArcDeathCapture.current();
        io.lunararcdevs.lunararc.common.event.LunarArcDeathCapture.Capture capture = io.lunararcdevs.lunararc.common.event.LunarArcDeathCapture.begin(entity);
        try {
            original.call(level, source);
        } finally {
            io.lunararcdevs.lunararc.common.event.LunarArcDeathCapture.end(previous);
        }

        java.util.List<org.bukkit.inventory.ItemStack> drops = new java.util.ArrayList<>(capture.items.size());
        for (net.minecraft.world.item.ItemStack stack : capture.items) {
            drops.add(org.bukkit.craftbukkit.inventory.CraftItemStack.asBukkitCopy(stack));
        }
        int dragonBase = 0;
        if (entity instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon dragon) {
            net.minecraft.world.level.dimension.end.EnderDragonFight fight = dragon.getDragonFight();
            dragonBase = fight != null && !fight.hasPreviouslyKilledDragon() ? 12000 : 500;
            capture.exp = dragonBase;
        }
        org.bukkit.event.entity.EntityDeathEvent event = CraftEventFactory.callEntityDeathEvent(entity, source, drops, capture.exp);
        if (event != null && event.isCancelled()) {
            double revive = event.getReviveHealth();
            if (revive <= 0.0D) revive = entity.getMaxHealth();
            entity.setHealth((float) Math.min(revive, entity.getMaxHealth()));
            this.dead = false;
            this.lunararc$deathCancelled = true;
            return;
        }
        java.util.List<org.bukkit.inventory.ItemStack> finalDrops = event == null ? drops : event.getDrops();
        for (org.bukkit.inventory.ItemStack drop : finalDrops) {
            if (drop == null || drop.getType().isAir() || drop.getAmount() <= 0) continue;
            if (entity.level() instanceof net.minecraft.server.level.ServerLevel dropLevel) entity.spawnAtLocation(dropLevel, org.bukkit.craftbukkit.inventory.CraftItemStack.asNMSCopy(drop));
        }
        int exp = event == null ? capture.exp : event.getDroppedExp();
        if (dragonBase > 0) {
            io.lunararcdevs.lunararc.common.event.LunarArcDeathCapture.setExpScale(entity, (double) exp / dragonBase);
            return;
        }
        if (exp > 0) net.minecraft.world.entity.ExperienceOrb.award(level, entity.position(), exp);
    }

    @WrapOperation(method = "updateFallFlying", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;setSharedFlag(IZ)V"))
    private void lunararc$toggleGlide(LivingEntity self, int flag, boolean value, Operation<Void> original) {
        if (flag == 7 && value != self.isFallFlying() && CraftEventFactory.callToggleGlideEvent(self, value).isCancelled()) {
            return;
        }
        original.call(self, flag, value);
    }
}
