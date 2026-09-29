package io.lunararcdevs.lunararc.common.mixin.core.entity;

import io.lunararcdevs.lunararc.common.LunarArcServerAccess;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Level;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(PrimedTnt.class)
public abstract class PrimedTntMixin {

    @Unique
    private static final int lunararc$DEFAULT_MAX_PER_TICK = 100;
    @Unique
    private static final int lunararc$DEFAULT_MAX_ACTIVE = 2000;
    @Unique
    private static final long lunararc$EXPLOSION_TIME_BUDGET_NANOS = 40_000_000L;
    @Unique
    private static final Map<String, long[]> lunararc$ticks = new ConcurrentHashMap<>();
    @Unique
    private static final Map<String, Deque<PrimedTnt>> lunararc$active = new ConcurrentHashMap<>();

    @Unique
    private boolean lunararc$registeredActive;
    @Unique
    private boolean lunararc$excess;

    @Shadow
    protected abstract void explode();

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void lunararc$registerActive(CallbackInfo ci) {
        PrimedTnt self = (PrimedTnt) (Object) this;
        Level level = self.level();
        if (level.isClientSide) return;
        if (!lunararc$registeredActive) {
            lunararc$registeredActive = true;
            lunararc$excess = !lunararc$acceptActive(self);
        }
        if (lunararc$excess) {
            if (lunararc$tryConsume(level)) {
                self.discard();
                this.explode();
            }
            ci.cancel();
        }
    }

    @Inject(
            method = "tick",
            cancellable = true,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/PrimedTnt;discard()V"),
            require = 0)
    private void lunararc$capExplosionsPerTick(CallbackInfo ci) {
        PrimedTnt self = (PrimedTnt) (Object) this;
        Level level = self.level();
        if (level.isClientSide) return;
        if (!lunararc$tryConsume(level)) {
            self.setFuse(1);
            ci.cancel();
        }
    }

    public float yield = 4.0F;
    public boolean isIncendiary = false;
    @Unique private org.bukkit.event.entity.ExplosionPrimeEvent lunararc$primeEvent;

    @Inject(method = "explode", at = @At("HEAD"), cancellable = true)
    private void lunararc$explosionPrime(CallbackInfo ci) {
        PrimedTnt self = (PrimedTnt) (Object) this;
        if (self.level().isClientSide) return;
        this.lunararc$primeEvent = org.bukkit.craftbukkit.event.CraftEventFactory.callExplosionPrimeEvent(self, this.yield, this.isIncendiary);
        if (this.lunararc$primeEvent.isCancelled()) ci.cancel();
    }

    @org.spongepowered.asm.mixin.injection.ModifyArg(method = "explode", index = 6, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;explode(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/level/ExplosionDamageCalculator;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;)Lnet/minecraft/world/level/Explosion;"))
    private float lunararc$primeRadius(float radius) {
        return this.lunararc$primeEvent == null ? radius : this.lunararc$primeEvent.getRadius();
    }

    @org.spongepowered.asm.mixin.injection.ModifyArg(method = "explode", index = 7, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;explode(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/level/ExplosionDamageCalculator;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;)Lnet/minecraft/world/level/Explosion;"))
    private boolean lunararc$primeFire(boolean fire) {
        return this.lunararc$primeEvent == null ? fire : this.lunararc$primeEvent.getFire();
    }

    @Unique
    private static boolean lunararc$tryConsume(Level level) {
        org.bukkit.World world;
        try {
            world = LunarArcServerAccess.getCraftWorld(level);
        } catch (Throwable notReady) {
            return true;
        }
        if (world == null) return true;

        // state: [0] game tick, [1] first grant nanos, [2] grants this tick, [3] per-tick max
        long[] state = lunararc$ticks.computeIfAbsent(world.getName(), name -> new long[]{Long.MIN_VALUE, 0, 0, lunararc$DEFAULT_MAX_PER_TICK});
        long gameTime = level.getGameTime();
        synchronized (state) {
            if (state[0] != gameTime) {
                state[0] = gameTime;
                state[2] = 0;
                state[3] = lunararc$readLimit(world.getName(), "max-tnt-per-tick", lunararc$DEFAULT_MAX_PER_TICK);
            }
            if (state[2] >= state[3]) return false;
            long now = System.nanoTime();
            if (state[2] == 0) {
                state[1] = now;
            } else if (now - state[1] > lunararc$EXPLOSION_TIME_BUDGET_NANOS) {
                return false;
            }
            state[2]++;
            return true;
        }
    }

    @Unique
    private static boolean lunararc$acceptActive(PrimedTnt tnt) {
        org.bukkit.World world;
        try {
            world = LunarArcServerAccess.getCraftWorld(tnt.level());
        } catch (Throwable notReady) {
            return true;
        }
        if (world == null) return true;

        Deque<PrimedTnt> active = lunararc$active.computeIfAbsent(world.getName(), name -> new ArrayDeque<>());
        int max = lunararc$readLimit(world.getName(), "max-tnt-active", lunararc$DEFAULT_MAX_ACTIVE);
        synchronized (active) {
            while (!active.isEmpty() && active.peekFirst().isRemoved()) {
                active.pollFirst();
            }
            if (active.size() >= max) return false;
            active.addLast(tnt);
            return true;
        }
    }

    @Unique
    private static int lunararc$readLimit(String worldName, String key, int fallback) {
        YamlConfiguration spigot;
        try {
            spigot = Bukkit.getServer().spigot().getSpigotConfig();
        } catch (Throwable unavailable) {
            return fallback;
        }
        if (spigot == null) return fallback;
        String perWorld = "world-settings." + worldName + "." + key;
        if (spigot.isInt(perWorld)) return spigot.getInt(perWorld);
        String shared = "world-settings.default." + key;
        if (spigot.isInt(shared)) return spigot.getInt(shared);
        return fallback;
    }
}
