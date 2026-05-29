package io.ampznetwork.lunararc.common.mixin.core.world;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mixin(Level.class)
public abstract class LevelMixin {

    private static final Logger LOGGER = LoggerFactory.getLogger("LunarArc");

    /**
     * Fire BlockPhysicsEvent when a block is updated due to neighbor changes.
     * Many plugins listen to this to detect redstone/piston interactions.
     */
    @Inject(method = "neighborChanged", at = @At("HEAD"), cancellable = true)
    private void lunararc$onNeighborChanged(BlockPos pos, net.minecraft.world.level.block.Block block,
            BlockPos fromPos, CallbackInfo ci) {
        try {
            org.bukkit.Server server = org.bukkit.Bukkit.getServer();
            if (server == null) return;

            Level self = (Level) (Object) this;
            if (!(self instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;

            org.bukkit.World world = server.getWorld(serverLevel.dimension().location().toString());
            if (world == null) return;

            org.bukkit.block.Block bukkitBlock =
                    org.bukkit.craftbukkit.v1_21_R1.block.CraftBlock.create(serverLevel, pos);

            org.bukkit.event.block.BlockPhysicsEvent event =
                    new org.bukkit.event.block.BlockPhysicsEvent(
                            bukkitBlock,
                            bukkitBlock.getBlockData(),
                            org.bukkit.craftbukkit.v1_21_R1.block.CraftBlock.create(serverLevel, fromPos));
            server.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                ci.cancel();
            }
        } catch (Throwable t) {
            // Never let Bukkit event errors crash the server
        }
    }

    /**
     * Fire BlockExplodeEvent when a non-entity explosion occurs (e.g. beds, TNT in blocks).
     */
    @Inject(method = "explode(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/level/ExplosionDamageCalculator;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;)Lnet/minecraft/world/level/Explosion;",
            at = @At("RETURN"))
    private void lunararc$onExplode(net.minecraft.world.entity.Entity source,
            net.minecraft.world.damagesource.DamageSource damageSource,
            net.minecraft.world.level.ExplosionDamageCalculator calculator,
            double x, double y, double z, float power, boolean fire,
            Level.ExplosionInteraction interaction,
            CallbackInfoReturnable<Explosion> cir) {
        try {
            if (source != null) return; // entity explosions handled in EntityMixin

            org.bukkit.Server server = org.bukkit.Bukkit.getServer();
            if (server == null) return;

            Level self = (Level) (Object) this;
            if (!(self instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;

            org.bukkit.World world = server.getWorld(serverLevel.dimension().location().toString());
            if (world == null) return;

            org.bukkit.Location location = new org.bukkit.Location(world, x, y, z);
            org.bukkit.block.Block block = world.getBlockAt((int) x, (int) y, (int) z);

            org.bukkit.event.block.BlockExplodeEvent event =
                    new org.bukkit.event.block.BlockExplodeEvent(block, new java.util.ArrayList<>(), power);
            server.getPluginManager().callEvent(event);
        } catch (Throwable t) {
            // Never let Bukkit event errors crash the server
        }
    }
}
