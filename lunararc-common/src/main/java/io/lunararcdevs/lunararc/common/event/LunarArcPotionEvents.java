package io.lunararcdevs.lunararc.common.event;

import io.lunararcdevs.lunararc.common.LunarArcServerAccess;
import io.lunararcdevs.lunararc.common.bridge.EntityBridge;
import io.lunararcdevs.lunararc.common.bridge.LivingEntityBridge;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.PotionSplashEvent;

/** PotionSplashEvent, WaterBottleSplashEvent and AreaEffectCloudApplyEvent. */
public final class LunarArcPotionEvents {
    private LunarArcPotionEvents() {}

    private static boolean ready(Entity entity) {
        return entity.level() instanceof ServerLevel && org.bukkit.Bukkit.isPrimaryThread();
    }

    private static org.bukkit.entity.LivingEntity living(LivingEntity entity) {
        return ((EntityBridge) entity).lunararc$getBukkitEntity() instanceof org.bukkit.entity.LivingEntity found ? found : null;
    }

    private static boolean fire(org.bukkit.event.Event event) {
        LunarArcServerAccess.getCraftServer().getPluginManager().callEvent(event);
        return event instanceof org.bukkit.event.Cancellable cancellable && cancellable.isCancelled();
    }

    private static org.bukkit.entity.Entity hitEntity(HitResult hit) {
        return hit instanceof EntityHitResult entityHit && ((EntityBridge) entityHit.getEntity()).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Entity found
                ? found : null;
    }

    private static org.bukkit.block.Block hitBlock(ServerLevel level, HitResult hit) {
        return hit instanceof BlockHitResult blockHit ? CraftBlock.at(level, blockHit.getBlockPos()) : null;
    }

    private static org.bukkit.block.BlockFace hitFace(HitResult hit) {
        return hit instanceof BlockHitResult blockHit ? CraftBlock.notchToBlockFace(blockHit.getDirection()) : null;
    }

    /**
     * A splash potion landed with effects. Returns true when this method applied the effects itself
     * (or a plugin cancelled the splash), so vanilla must be skipped; false when nobody listens.
     */
    public static boolean splash(AbstractThrownPotion potion, ServerLevel level, ItemStack stack, HitResult hit) {
        if (PotionSplashEvent.getHandlerList().getRegisteredListeners().length == 0 || !ready(potion)) return false;
        if (!(((EntityBridge) potion).lunararc$getBukkitEntity() instanceof org.bukkit.entity.ThrownPotion bukkitPotion)) return false;

        PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        float durationScale = stack.getOrDefault(DataComponents.POTION_DURATION_SCALE, 1.0F);
        AABB box = potion.getBoundingBox().move(hit.getLocation().subtract(potion.position()));
        AABB wide = box.inflate(4.0, 2.0, 4.0);
        float margin = ProjectileUtil.computeMargin(potion);

        Map<org.bukkit.entity.LivingEntity, Double> intensities = new HashMap<>();
        Map<org.bukkit.entity.LivingEntity, LivingEntity> handles = new IdentityHashMap<>();
        for (LivingEntity target : potion.level().getEntitiesOfClass(LivingEntity.class, wide)) {
            if (!target.isAffectedByPotions()) continue;
            double distance = box.distanceToSqr(target.getBoundingBox().inflate(margin));
            if (distance >= 16.0) continue;
            org.bukkit.entity.LivingEntity bukkit = living(target);
            if (bukkit == null) continue;
            intensities.put(bukkit, 1.0 - Math.sqrt(distance) / 4.0);
            handles.put(bukkit, target);
        }

        PotionSplashEvent event = new PotionSplashEvent(bukkitPotion, hitEntity(hit), hitBlock(level, hit), hitFace(hit), intensities);
        if (fire(event)) return true;

        Entity source = potion.getEffectSource();
        for (org.bukkit.entity.LivingEntity affected : event.getAffectedEntities()) {
            LivingEntity target = handles.get(affected);
            if (target == null) continue;
            double intensity = event.getIntensity(affected);
            for (MobEffectInstance effect : contents.getAllEffects()) {
                Holder<MobEffect> holder = effect.getEffect();
                if (holder.value().isInstantaneous()) {
                    holder.value().applyInstantaneousEffect(level, potion, potion.getOwner(), target, effect.getAmplifier(), intensity);
                } else {
                    int duration = effect.mapDuration(base -> (int) (intensity * base * durationScale + 0.5));
                    MobEffectInstance scaled = new MobEffectInstance(holder, duration, effect.getAmplifier(), effect.isAmbient(), effect.isVisible());
                    if (!scaled.endsWithin(20)) {
                        ((LivingEntityBridge) target).lunararc$pushEffectCause(EntityPotionEffectEvent.Cause.POTION_SPLASH);
                        target.addEffect(scaled, source);
                    }
                }
            }
        }
        return true;
    }

    /** A water bottle landed. Returns true when this method applied the water effects itself (or a plugin cancelled them). */
    public static boolean waterSplash(AbstractThrownPotion potion, ServerLevel level, HitResult hit) {
        if (io.papermc.paper.event.entity.WaterBottleSplashEvent.getHandlerList().getRegisteredListeners().length == 0 || !ready(potion)) return false;
        if (!(((EntityBridge) potion).lunararc$getBukkitEntity() instanceof org.bukkit.entity.ThrownPotion bukkitPotion)) return false;

        AABB box = potion.getBoundingBox().inflate(4.0, 2.0, 4.0);
        Map<org.bukkit.entity.LivingEntity, Double> affected = new HashMap<>();
        Set<org.bukkit.entity.LivingEntity> rehydrate = new HashSet<>();
        Set<org.bukkit.entity.LivingEntity> extinguish = new HashSet<>();
        for (LivingEntity target : potion.level().getEntitiesOfClass(LivingEntity.class, box,
                entity -> AbstractThrownPotion.WATER_SENSITIVE_OR_ON_FIRE.test(entity) || entity instanceof Axolotl)) {
            org.bukkit.entity.LivingEntity bukkit = living(target);
            if (bukkit == null) continue;
            if (target instanceof Axolotl) rehydrate.add(bukkit);
            if (potion.distanceToSqr(target) >= 16.0) continue;
            if (target.isSensitiveToWater()) affected.put(bukkit, 1.0);
            if (target.isOnFire() && target.isAlive()) extinguish.add(bukkit);
        }

        io.papermc.paper.event.entity.WaterBottleSplashEvent event = new io.papermc.paper.event.entity.WaterBottleSplashEvent(
                bukkitPotion, hitEntity(hit), hitBlock(level, hit), hitFace(hit), affected, rehydrate, extinguish);
        if (fire(event)) return true;

        for (org.bukkit.entity.LivingEntity damaged : event.getToDamage()) {
            LivingEntity handle = handle(damaged);
            if (handle != null) handle.hurtServer(level, potion.damageSources().indirectMagic(potion, potion.getOwner()), 1.0F);
        }
        for (org.bukkit.entity.LivingEntity burning : event.getToExtinguish()) {
            LivingEntity handle = handle(burning);
            if (handle != null) handle.extinguishFire();
        }
        for (org.bukkit.entity.LivingEntity thirsty : event.getToRehydrate()) {
            if (handle(thirsty) instanceof Axolotl axolotl) axolotl.rehydrate();
        }
        return true;
    }

    private static LivingEntity handle(org.bukkit.entity.LivingEntity entity) {
        return entity instanceof org.bukkit.craftbukkit.entity.CraftLivingEntity craft ? craft.getHandle() : null;
    }

    /**
     * Narrows the entities an area effect cloud may affect this tick. Returns the list to use: the
     * candidates unchanged when nobody listens, an empty list when a plugin cancelled the application.
     */
    public static List<LivingEntity> cloudApply(AreaEffectCloud cloud, List<LivingEntity> candidates) {
        if (candidates.isEmpty() || org.bukkit.event.entity.AreaEffectCloudApplyEvent.getHandlerList().getRegisteredListeners().length == 0
                || !ready(cloud)) return candidates;
        if (!(((EntityBridge) cloud).lunararc$getBukkitEntity() instanceof org.bukkit.entity.AreaEffectCloud bukkitCloud)) return candidates;

        float radius = cloud.getRadius();
        List<org.bukkit.entity.LivingEntity> bukkitList = new ArrayList<>();
        Map<org.bukkit.entity.LivingEntity, LivingEntity> handles = new IdentityHashMap<>();
        for (LivingEntity target : candidates) {
            if (!target.isAffectedByPotions()) continue;
            double dx = target.getX() - cloud.getX();
            double dz = target.getZ() - cloud.getZ();
            if (dx * dx + dz * dz > radius * radius) continue;
            org.bukkit.entity.LivingEntity bukkit = living(target);
            if (bukkit == null) continue;
            bukkitList.add(bukkit);
            handles.put(bukkit, target);
        }
        if (bukkitList.isEmpty()) return candidates;

        org.bukkit.event.entity.AreaEffectCloudApplyEvent event = new org.bukkit.event.entity.AreaEffectCloudApplyEvent(bukkitCloud, bukkitList);
        if (fire(event)) return List.of();

        List<LivingEntity> kept = new ArrayList<>();
        for (org.bukkit.entity.LivingEntity bukkit : event.getAffectedEntities()) {
            LivingEntity handle = handles.get(bukkit);
            if (handle != null) kept.add(handle);
        }
        return kept;
    }
}
