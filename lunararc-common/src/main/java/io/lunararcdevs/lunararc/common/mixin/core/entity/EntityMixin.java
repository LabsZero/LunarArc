package io.lunararcdevs.lunararc.common.mixin.core.entity;

import org.spongepowered.asm.mixin.injection.ModifyVariable;
import io.lunararcdevs.lunararc.common.bridge.CommandSourceBridge;
import io.lunararcdevs.lunararc.common.bridge.EntityBridge;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.persistence.CraftPersistentDataContainer;
import org.bukkit.persistence.PersistentDataContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin implements EntityBridge, CommandSourceBridge {

    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(
            method = "teleportToPortalDestination",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;canTeleport(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/Level;)Z"),
            require = 1)
    private boolean lunararc$playersMayAlwaysChangeDimension(Entity entity, Level from, Level to,
            Operation<Boolean> original) {
        boolean vanilla = original.call(entity, from, to);
        if (vanilla || !((Object) this instanceof net.minecraft.server.level.ServerPlayer)) return vanilla;
        return true;
    }

    @Shadow private int portalCooldown;
    @Shadow private int remainingFireTicks;
    @Shadow private boolean hasVisualFire;
    @Shadow private Entity vehicle;
    public boolean generation;
    public boolean valid;
    public org.bukkit.projectiles.ProjectileSource projectileSource;
    @Shadow public abstract net.minecraft.world.level.Level level();

    @Override public int lunararc$getPortalCooldown() { return this.portalCooldown; }
    @Override public void lunararc$setPortalCooldown(int cooldown) { this.portalCooldown = cooldown; }
    @Override public int lunararc$getRemainingFireTicks() { return this.remainingFireTicks; }
    @Override public void lunararc$setRemainingFireTicks(int ticks) { this.remainingFireTicks = ticks; }

    private org.bukkit.entity.Entity lunararc$bukkitEntity;
    private CreatureSpawnEvent.SpawnReason lunararc$spawnReason;
    private boolean lunararc$fromMobSpawner;
    private Vec3 lunararc$origin;
    private java.util.UUID lunararc$originWorld;
    private boolean lunararc$inWorld;
    private boolean lunararc$persistent = true;
    @org.spongepowered.asm.mixin.injection.Inject(method = "isAlliedTo(Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void lunararc$additionalAlliedRules(Entity other, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        // only entities a plugin already wrapped can carry rules, so never create wrappers here
        if (this.lunararc$bukkitEntity instanceof CraftEntity craft && other != null && craft.considersEntityAsAlly0(
                ((io.lunararcdevs.lunararc.common.bridge.EntityBridge) other).lunararc$getBukkitEntity())) {
            cir.setReturnValue(true);
        }
    }

    @Override public void lunararc$setBukkitEntity(org.bukkit.entity.Entity entity) { this.lunararc$bukkitEntity = entity; }
    @Override public org.bukkit.entity.Entity lunararc$peekBukkitEntity() { return this.lunararc$bukkitEntity; }
    @Override public CreatureSpawnEvent.SpawnReason lunararc$getSpawnReason() { return this.lunararc$spawnReason; }
    @Override public void lunararc$setSpawnReason(CreatureSpawnEvent.SpawnReason reason) { this.lunararc$spawnReason = reason; }
    @Override public boolean lunararc$fromMobSpawner() { return this.lunararc$fromMobSpawner; }
    @Override public void lunararc$setFromMobSpawner(boolean fromMobSpawner) { this.lunararc$fromMobSpawner = fromMobSpawner; }
    @Override public Vec3 lunararc$getOrigin() { return this.lunararc$origin; }
    @Override public java.util.UUID lunararc$getOriginWorld() { return this.lunararc$originWorld; }
    @Override public void lunararc$setOrigin(Vec3 origin, java.util.UUID worldId) {
        this.lunararc$origin = origin;
        this.lunararc$originWorld = worldId;
    }
    @Override public boolean lunararc$isInWorld() { return this.lunararc$inWorld; }
    @Override public void lunararc$setInWorld(boolean inWorld) {
        this.lunararc$inWorld = inWorld;
        this.valid = inWorld;
    }
    @Override public org.bukkit.projectiles.ProjectileSource lunararc$getProjectileSource() { return this.projectileSource; }
    @Override public void lunararc$setProjectileSource(org.bukkit.projectiles.ProjectileSource source) { this.projectileSource = source; }

    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void lunararc$movementLock(net.minecraft.world.entity.MoverType type, Vec3 movement, CallbackInfo ci) {
        if ((Object) this instanceof io.lunararcdevs.lunararc.common.bridge.entity.MovementLockBridge lock && !lock.lunararc$canMove()) {
            ci.cancel();
        }
    }
    /** Paper's tri-state visual fire override (TRUE forces fire, FALSE hides it even when burning, NOT_SET is vanilla). */
    public net.kyori.adventure.util.TriState visualFire = net.kyori.adventure.util.TriState.NOT_SET;
    @Override public boolean lunararc$isVisualFire() { return this.visualFire == net.kyori.adventure.util.TriState.TRUE || this.hasVisualFire; }
    @Override public void lunararc$setVisualFire(boolean visualFire) {
        this.hasVisualFire = visualFire;
        this.visualFire = visualFire ? net.kyori.adventure.util.TriState.TRUE : net.kyori.adventure.util.TriState.NOT_SET;
    }
    @Override public net.kyori.adventure.util.TriState lunararc$getVisualFireState() {
        return this.visualFire == net.kyori.adventure.util.TriState.NOT_SET && this.hasVisualFire ? net.kyori.adventure.util.TriState.TRUE : this.visualFire;
    }
    @Override public void lunararc$setVisualFireState(net.kyori.adventure.util.TriState state) {
        this.visualFire = java.util.Objects.requireNonNull(state, "state");
        this.hasVisualFire = state == net.kyori.adventure.util.TriState.TRUE;
    }
    @ModifyVariable(method = "setSharedFlagOnFire", at = @At("HEAD"), argsOnly = true, require = 0)
    private boolean lunararc$visualFireState(boolean isOnFire) {
        return this.visualFire.toBooleanOrElse(isOnFire);
    }
    @org.spongepowered.asm.mixin.Shadow private Entity.RemovalReason removalReason;
    @Override public boolean lunararc$isPersistent() { return this.lunararc$persistent; }
    @Override public void lunararc$setPersistent(boolean persistent) { this.lunararc$persistent = persistent; }
    @Override public void lunararc$setLevel(Level level) { ((Entity) (Object) this).setLevel(level); }
    @Override public boolean lunararc$saveAsPassenger(CompoundTag tag) {
        Entity self = (Entity) (Object) this;
        try (net.minecraft.util.ProblemReporter.ScopedCollector problems = new net.minecraft.util.ProblemReporter.ScopedCollector(self.problemPath(), org.slf4j.LoggerFactory.getLogger(EntityMixin.class))) {
            net.minecraft.world.level.storage.TagValueOutput output = net.minecraft.world.level.storage.TagValueOutput.createWithContext(problems, self.registryAccess());
            // Paper snapshots entities that are already being removed (e.g. plugins that pick up a mob), so do not let the
            // removal reason veto the save.
            Entity.RemovalReason reason = this.removalReason;
            if (reason != null && !reason.shouldSave()) this.removalReason = null;
            boolean saved;
            try {
                saved = self.saveAsPassenger(output);
            } finally {
                if (reason != null) this.removalReason = reason;
            }
            if (!saved) return false;
            tag.merge(output.buildResult());
            return true;
        }
    }

    private CraftPersistentDataContainer lunararc$pdc = new CraftPersistentDataContainer();
    @Override public PersistentDataContainer lunararc$getPersistentDataContainer() { return lunararc$pdc; }
    @Override public org.bukkit.entity.Entity lunararc$getBukkitEntity() {
        if (lunararc$bukkitEntity == null) {
            net.minecraft.server.MinecraftServer minecraftServer = ((Entity) (Object) this).level().getServer();
            if (minecraftServer == null) {
                throw new IllegalStateException("Cannot create a Bukkit entity wrapper without a server");
            }
            lunararc$bukkitEntity = CraftEntity.getEntity(
                    io.lunararcdevs.lunararc.common.LunarArcServerAccess.getCraftServer(minecraftServer),
                    (Entity) (Object) this);
        }
        return lunararc$bukkitEntity;
    }

    public CraftEntity getBukkitEntity() {
        return (CraftEntity) this.lunararc$getBukkitEntity();
    }


    @Override
    public org.bukkit.command.CommandSender lunararc$getBukkitSender(net.minecraft.commands.CommandSourceStack stack) {
        org.bukkit.entity.Entity entity = this.lunararc$getBukkitEntity();
        if (entity instanceof org.bukkit.command.CommandSender sender) {
            return sender;
        }
        throw new IllegalStateException("Entity command source is not a Bukkit CommandSender: " + entity.getType());
    }

    @Inject(method = "load", at = @At("RETURN"), require = 1)
    private void lunararc$loadEntityData(net.minecraft.world.level.storage.ValueInput input, CallbackInfo ci) {
        input.read("BukkitValues", net.minecraft.nbt.CompoundTag.CODEC).ifPresent(lunararc$pdc::fromTag);
        this.lunararc$fromMobSpawner = input.getBooleanOr("Paper.FromMobSpawner", false);
        input.getString("Paper.SpawnReason").ifPresent(name -> {
            try {
                this.lunararc$spawnReason = CreatureSpawnEvent.SpawnReason.valueOf(name);
            } catch (IllegalArgumentException exception) {
                this.lunararc$spawnReason = CreatureSpawnEvent.SpawnReason.DEFAULT;
            }
        });
        java.util.List<Double> origin = input.read("Paper.Origin", com.mojang.serialization.Codec.DOUBLE.listOf()).orElse(java.util.List.of());
        if (origin.size() >= 3) {
            this.lunararc$origin = new Vec3(origin.get(0), origin.get(1), origin.get(2));
            this.lunararc$originWorld = input.read("Paper.OriginWorld", net.minecraft.core.UUIDUtil.CODEC).orElse(null);
        }
        this.lunararc$persistent = input.getBooleanOr("Bukkit.persist", true);
        if (this.lunararc$spawnReason == null) {
            Entity entity = (Entity) (Object) this;
            if (this.lunararc$fromMobSpawner) {
                this.lunararc$spawnReason = CreatureSpawnEvent.SpawnReason.SPAWNER;
            } else if (entity instanceof Mob mob
                    && (entity instanceof Animal || entity instanceof AbstractFish)
                    && !mob.removeWhenFarAway(0.0)
                    && !input.getBooleanOr("PersistenceRequired", false)) {
                this.lunararc$spawnReason = CreatureSpawnEvent.SpawnReason.NATURAL;
            } else {
                this.lunararc$spawnReason = CreatureSpawnEvent.SpawnReason.DEFAULT;
            }
        }
    }

    @Inject(method = "saveWithoutId", at = @At("RETURN"), require = 1)
    private void lunararc$saveEntityData(net.minecraft.world.level.storage.ValueOutput output, CallbackInfo ci) {
        net.minecraft.nbt.CompoundTag bukkitValues = lunararc$pdc.toTag();
        if (!bukkitValues.isEmpty()) {
            output.store("BukkitValues", net.minecraft.nbt.CompoundTag.CODEC, bukkitValues);
        }
        if (!this.lunararc$persistent) {
            output.putBoolean("Bukkit.persist", false);
        }
        if (this.lunararc$spawnReason != null) {
            output.putString("Paper.SpawnReason", this.lunararc$spawnReason.name());
        }
        if (this.lunararc$fromMobSpawner) {
            output.putBoolean("Paper.FromMobSpawner", true);
        }
        if (this.lunararc$origin != null) {
            if (this.lunararc$originWorld != null) {
                output.store("Paper.OriginWorld", net.minecraft.core.UUIDUtil.CODEC, this.lunararc$originWorld);
            }
            output.store("Paper.Origin", com.mojang.serialization.Codec.DOUBLE.listOf(),
                    java.util.List.of(this.lunararc$origin.x, this.lunararc$origin.y, this.lunararc$origin.z));
        }
    }
    @Inject(method = "startRiding(Lnet/minecraft/world/entity/Entity;ZZ)Z", cancellable = true,
            at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/world/entity/Entity;isPassenger()Z"))
    private void lunararc$vehicleEnter(Entity vehicle, boolean force, boolean sendGameEvent, CallbackInfoReturnable<Boolean> cir) {
        if (!this.lunararc$inWorld || level().isClientSide) return;
        org.bukkit.entity.Entity bukkitVehicle = ((EntityBridge) vehicle).lunararc$getBukkitEntity();
        org.bukkit.entity.Entity bukkitPassenger = this.lunararc$getBukkitEntity();
        var pluginManager = org.bukkit.Bukkit.getPluginManager();
        if (bukkitVehicle instanceof org.bukkit.entity.Vehicle bukkitVehicleEntity
                && bukkitPassenger instanceof org.bukkit.entity.LivingEntity) {
            var event = new org.bukkit.event.vehicle.VehicleEnterEvent(bukkitVehicleEntity, bukkitPassenger);
            pluginManager.callEvent(event);
            if (event.isCancelled()) {
                cir.setReturnValue(false);
                return;
            }
        }
        var event = new org.bukkit.event.entity.EntityMountEvent(bukkitPassenger, bukkitVehicle);
        pluginManager.callEvent(event);
        if (event.isCancelled()) {
            cir.setReturnValue(false);
        }
    }

    @WrapOperation(method = "removeVehicle",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;removePassenger(Lnet/minecraft/world/entity/Entity;)V"))
    private void lunararc$vehicleExit(Entity vehicle, Entity passenger, Operation<Void> original) {
        if (lunararc$dismountCancelled(vehicle)) {
            this.vehicle = vehicle;
            return;
        }
        original.call(vehicle, passenger);
    }

    @Unique
    private boolean lunararc$dismountCancelled(Entity vehicle) {
        if (!this.lunararc$inWorld || level().isClientSide) return false;
        org.bukkit.entity.Entity bukkitVehicle = ((EntityBridge) vehicle).lunararc$getBukkitEntity();
        org.bukkit.entity.Entity bukkitPassenger = this.lunararc$getBukkitEntity();
        var pluginManager = org.bukkit.Bukkit.getPluginManager();
        if (bukkitVehicle instanceof org.bukkit.entity.Vehicle bukkitVehicleEntity
                && bukkitPassenger instanceof org.bukkit.entity.LivingEntity livingPassenger) {
            var event = new org.bukkit.event.vehicle.VehicleExitEvent(bukkitVehicleEntity, livingPassenger, true);
            pluginManager.callEvent(event);
            if (event.isCancelled() && this.vehicle == null) return true;
        }
        var event = new org.bukkit.event.entity.EntityDismountEvent(bukkitPassenger, bukkitVehicle, true);
        pluginManager.callEvent(event);
        return event.isCancelled();
    }

    @WrapOperation(method = "interact", require = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Leashable;dropLeash()V"))
    private void lunararc$playerUnleash(net.minecraft.world.entity.Leashable leashable, Operation<Void> original,
            @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) net.minecraft.world.entity.player.Player player,
            @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) net.minecraft.world.InteractionHand hand) {
        Entity self = (Entity) (Object) this;
        Object bukkit = this.lunararc$getBukkitEntity();
        Object bukkitPlayer = ((EntityBridge) player).lunararc$getBukkitEntity();
        if (!(bukkit instanceof org.bukkit.entity.Entity bukkitEntity)
                || !(bukkitPlayer instanceof org.bukkit.entity.Player human)) {
            original.call(leashable);
            return;
        }
        var event = new org.bukkit.event.player.PlayerUnleashEntityEvent(
                bukkitEntity, human, org.bukkit.craftbukkit.CraftEquipmentSlot.getHand(hand),
                !player.hasInfiniteMaterials());
        org.bukkit.Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket(
                        self, leashable.getLeashHolder()));
            }
            return;
        }
        if (event.isDropLeash()) leashable.dropLeash(); else leashable.removeLeash();
    }

    @Unique private boolean lunararc$combustEventFired;

    @WrapOperation(method = "thunderHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;igniteForSeconds(F)V"))
    private void lunararc$lightningCombust(Entity self, float seconds, Operation<Void> original,
            net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.LightningBolt lightning) {
        var event = new org.bukkit.event.entity.EntityCombustByEntityEvent(
                ((EntityBridge) lightning).lunararc$getBukkitEntity(), this.lunararc$getBukkitEntity(), seconds);
        org.bukkit.Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;
        this.lunararc$combustEventFired = true;
        try {
            original.call(self, event.getDuration());
        } finally {
            this.lunararc$combustEventFired = false;
        }
    }

    // HEAD, not the hurtServer call: NeoForge rewrites thunderHit to damage through Entity#hurt instead.
    @Inject(method = "thunderHit", cancellable = true, at = @At("HEAD"))
    private void lunararc$lightningHangingBreak(net.minecraft.server.level.ServerLevel level,
            net.minecraft.world.entity.LightningBolt lightning, CallbackInfo ci) {
        if (this.lunararc$getBukkitEntity() instanceof org.bukkit.entity.Hanging hanging) {
            var event = new org.bukkit.event.hanging.HangingBreakByEntityEvent(hanging,
                    ((EntityBridge) lightning).lunararc$getBukkitEntity(),
                    new org.bukkit.craftbukkit.damage.CraftDamageSource(lightning.damageSources().lightningBolt()));
            org.bukkit.Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) ci.cancel();
        }
    }

    @WrapMethod(method = "igniteForSeconds")
    private void lunararc$combust(float seconds, Operation<Void> original) {
        Entity self = (Entity) (Object) this;
        if (self.level().isClientSide || !org.bukkit.Bukkit.isPrimaryThread() || this.lunararc$combustEventFired
                || io.lunararcdevs.lunararc.common.event.LunarArcMobEvents.combustSuppressed) {
            original.call(seconds);
            return;
        }
        var event = new org.bukkit.event.entity.EntityCombustEvent(this.lunararc$getBukkitEntity(), seconds);
        org.bukkit.Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            original.call(event.getDuration());
        }
    }

    @Inject(method = "remove", at = @At("HEAD"), require = 0)
    private void lunararc$markRemoved(Entity.RemovalReason reason, CallbackInfo ci) {
        io.lunararcdevs.lunararc.common.event.LunarArcPaperEvents.removed((Entity) (Object) this, reason);
        this.lunararc$inWorld = false;
    }

    @Shadow @org.spongepowered.asm.mixin.Final private static net.minecraft.network.syncher.EntityDataAccessor<Integer> DATA_AIR_SUPPLY_ID;
    @Shadow @org.spongepowered.asm.mixin.Final protected net.minecraft.network.syncher.SynchedEntityData entityData;

    @Inject(method = "setPose", at = @At("HEAD"), require = 0)
    private void lunararc$poseChange(net.minecraft.world.entity.Pose pose, CallbackInfo ci) {
        io.lunararcdevs.lunararc.common.event.LunarArcPaperEvents.poseChange((Entity) (Object) this, pose);
    }

    @Inject(method = "setAirSupply", at = @At("HEAD"), cancellable = true, require = 0)
    private void lunararc$airChange(int amount, CallbackInfo ci) {
        int changed = io.lunararcdevs.lunararc.common.event.LunarArcPaperEvents.airChange((Entity) (Object) this, amount);
        if (changed == amount) return;
        ci.cancel();
        this.entityData.set(DATA_AIR_SUPPLY_ID, changed);
    }

    @Inject(method = "setSwimming", at = @At("HEAD"), cancellable = true, require = 0)
    private void lunararc$toggleSwim(boolean swimming, CallbackInfo ci) {
        if (io.lunararcdevs.lunararc.common.event.LunarArcPaperEvents.toggleSwim((Entity) (Object) this, swimming)) ci.cancel();
    }



}
