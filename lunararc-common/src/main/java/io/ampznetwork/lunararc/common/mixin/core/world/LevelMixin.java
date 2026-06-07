package io.ampznetwork.lunararc.common.mixin.core.world;

import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Level.class)
public abstract class LevelMixin {

    /**
     * Fire BlockPhysicsEvent when a block receives a neighbor update.
     * The third parameter is LevelReader in 1.21.x (nullable), not BlockPos.
     */
    @Inject(method = "neighborChanged(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;Lnet/minecraft/core/BlockPos;)V",
            at = @At("HEAD"), cancellable = true, require = 0)
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
                            bukkitBlock.getBlockData());
            server.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                ci.cancel();
            }
        } catch (Throwable t) {
            // Never let event errors crash the server
        }
    }
}
