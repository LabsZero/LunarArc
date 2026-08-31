package io.ampznetwork.lunararc.common.mixin.core.world;

import io.ampznetwork.lunararc.common.bridge.ExplosionBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Fires {@link org.bukkit.event.entity.EntityExplodeEvent} and
 * {@link org.bukkit.event.block.BlockExplodeEvent}.
 *
 * <p>Neither event existed on LunarArc before this, so every explosion - TNT, creepers, ghast
 * fireballs, end crystals, beds in the Nether - went straight through to block destruction with no
 * plugin ever consulted. Land-protection plugins (WorldGuard, GriefPrevention, Towny, Lands and
 * the rest) do all of their explosion protection through exactly these two events, so on a server
 * running any of them claims were unprotected against explosives while looking protected against
 * everything else.</p>
 *
 * <p>Shape follows CraftBukkit's own patch to {@code Explosion#finalizeExplosion}, cross-checked
 * against MohistMC/Youer's 1.21.1 hybrid port of it: build the Bukkit block list from
 * {@code toBlow} in reverse skipping air, fire the entity variant when the explosion has a source
 * entity and the block variant when it does not, then replace {@code toBlow} with whatever the
 * event's {@code blockList()} still contains.</p>
 *
 * <p>Two deliberate departures from where CraftBukkit puts this, both of which keep the observable
 * behaviour identical while asking less of the loader's copy of the class:</p>
 * <ul>
 *   <li>It hooks the head of {@code finalizeExplosion} rather than the middle of it. Everything
 *       that populates {@code toBlow} - vanilla's own ray casts and NeoForge's and Forge's
 *       explosion hooks alike - has already run by the time this method is entered, so the list a
 *       plugin sees is the same one CraftBukkit would show it, and a mod and a plugin can each
 *       remove blocks without either losing the other's edits. Anchoring on the head also means no
 *       dependence on an interior instruction that a loader might have moved.</li>
 *   <li>A cancelled event empties {@code toBlow} instead of returning from the method. Everything
 *       left in {@code finalizeExplosion} - block breaking, drops, and fire spread - iterates that
 *       list, so an empty one destroys nothing, which is what cancelling means. It also leaves the
 *       explosion's sound and particles intact, which is what CraftBukkit does too: its own return
 *       happens after they have already been played.</li>
 * </ul>
 *
 * <p>Known limit: a plugin calling {@code setYield} has its value stored and handed back, but it
 * does not yet change how many blocks drop as items. CraftBukkit spends the yield inside
 * {@code BlockBehaviour#onExplosionHit} when it builds the loot context - a separate injection
 * into a method the loaders also patch, left for its own change rather than guessed at here.</p>
 */
@Mixin(Explosion.class)
public abstract class ExplosionMixin implements ExplosionBridge {

    @Shadow @Final private Level level;
    @Shadow @Final private double x;
    @Shadow @Final private double y;
    @Shadow @Final private double z;
    @Shadow @Final private Entity source;

    // Accessors rather than field shadows: toBlow is declared as a fastutil ObjectArrayList and
    // radius as a private field, and a shadow has to match the declared type exactly. Both public
    // methods are part of the vanilla surface the loaders' own explosion events already use.
    @Shadow public abstract List<BlockPos> getToBlow();

    @Shadow public abstract float radius();

    @Shadow public abstract Explosion.BlockInteraction getBlockInteraction();

    // Computed on first read rather than in <init>: Explosion has several constructors that
    // delegate to one another, so an <init> injection would run more than once per explosion.
    @Unique private Float lunararc$yield;

    @Override
    public float lunararc$getYield() {
        if (this.lunararc$yield == null) {
            // CraftBukkit's own initializer, kept verbatim: a decaying explosion reports the same
            // fraction vanilla would have dropped, so an untouched explosion looks unchanged.
            this.lunararc$yield = this.getBlockInteraction() == Explosion.BlockInteraction.DESTROY_WITH_DECAY
                    ? 1.0F / this.radius()
                    : 1.0F;
        }
        return this.lunararc$yield;
    }

    @Override
    public void lunararc$setYield(float yield) {
        this.lunararc$yield = yield;
    }

    @Inject(method = "finalizeExplosion", at = @At("HEAD"))
    private void lunararc$callExplosionEvent(boolean spawnParticles, CallbackInfo ci) {
        // Vanilla only touches blocks when the interaction is not KEEP, and CraftBukkit fires the
        // event inside that same branch - a KEEP explosion breaks nothing, so there is nothing for
        // a protection plugin to veto.
        if (!(this.level instanceof ServerLevel serverLevel)) return;
        if (this.getBlockInteraction() == Explosion.BlockInteraction.KEEP) return;

        List<BlockPos> toBlow = this.getToBlow();
        List<org.bukkit.block.Block> blockList = new ArrayList<>(toBlow.size());
        for (int index = toBlow.size() - 1; index >= 0; index--) {
            org.bukkit.block.Block block = CraftBlock.at(serverLevel, toBlow.get(index));
            if (!block.getType().isAir()) blockList.add(block);
        }

        boolean cancelled;
        List<org.bukkit.block.Block> remaining;
        if (this.source != null) {
            org.bukkit.event.entity.EntityExplodeEvent event = CraftEventFactory.callEntityExplodeEvent(
                    this.source, blockList, this.lunararc$getYield(), this.getBlockInteraction());
            cancelled = event.isCancelled();
            remaining = event.blockList();
            this.lunararc$setYield(event.getYield());
        } else {
            // CraftBukkit reads the pre-explosion state off the damage source here so a bed or
            // respawn anchor reports the block that actually blew up. That field is a CraftBukkit
            // addition to DamageSource which LunarArc has no bridge for, so the block's current
            // state is used instead - the same fallback CraftBukkit takes when the source carries
            // no state of its own.
            org.bukkit.block.Block block = CraftBlock.at(serverLevel, BlockPos.containing(this.x, this.y, this.z));
            org.bukkit.event.block.BlockExplodeEvent event = CraftEventFactory.callBlockExplodeEvent(
                    block, block.getState(), blockList, this.lunararc$getYield(), this.getBlockInteraction());
            cancelled = event.isCancelled();
            remaining = event.blockList();
            this.lunararc$setYield(event.getYield());
        }

        toBlow.clear();
        if (cancelled) return;
        for (org.bukkit.block.Block block : remaining) {
            toBlow.add(new BlockPos(block.getX(), block.getY(), block.getZ()));
        }
    }
}
