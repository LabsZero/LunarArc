package io.lunararcdevs.lunararc.common.mixin.core.world.block;

import io.lunararcdevs.lunararc.common.bridge.ServerLevelBridge;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.WitherSkullBlock;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A built wither reports BUILD_WITHER and a cancelled spawn leaves the structure intact. */
@Mixin(WitherSkullBlock.class)
public abstract class WitherBuildSpawnMixin {
    @Shadow
    private static BlockPattern getOrCreateWitherFull() { throw new AssertionError(); }

    @Inject(method = "checkSpawn(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/SkullBlockEntity;)V",
            at = @At("HEAD"), cancellable = true)
    private static void lunararc$buildWither(Level level, BlockPos pos, SkullBlockEntity blockEntity, CallbackInfo ci) {
        ci.cancel();
        if (level.isClientSide()) return;
        BlockState state = blockEntity.getBlockState();
        boolean skull = state.is(Blocks.WITHER_SKELETON_SKULL) || state.is(Blocks.WITHER_SKELETON_WALL_SKULL);
        if (!skull || pos.getY() < level.getMinY() || level.getDifficulty() == Difficulty.PEACEFUL) return;
        BlockPattern.BlockPatternMatch match = getOrCreateWitherFull().find(level, pos);
        if (match == null) return;
        WitherBoss wither = net.minecraft.world.entity.EntityTypes.WITHER.create(level, EntitySpawnReason.TRIGGERED);
        if (wither == null) return;
        BlockPos spawn = match.getBlock(1, 2, 0).getPos();
        float yaw = match.getForwards().getAxis() == Direction.Axis.X ? 0.0F : 90.0F;
        wither.snapTo(spawn.getX() + 0.5, spawn.getY() + 0.55, spawn.getZ() + 0.5, yaw, 0.0F);
        wither.yBodyRot = yaw;
        wither.makeInvulnerable();
        if (!io.lunararcdevs.lunararc.common.event.LunarArcGameplayEvents.construct(level, wither, match)) return;
        if (!((ServerLevelBridge) level).lunararc$addFreshEntity(wither, CreatureSpawnEvent.SpawnReason.BUILD_WITHER)) return;
        CarvedPumpkinBlock.clearPatternBlocks(level, match);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, wither.getBoundingBox().inflate(50.0))) {
            CriteriaTriggers.SUMMONED_ENTITY.trigger(player, wither);
        }
        CarvedPumpkinBlock.updatePatternBlocks(level, match);
    }
}
