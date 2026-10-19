package io.lunararcdevs.lunararc.common.mixin.core.world.block;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import io.lunararcdevs.lunararc.common.bridge.ServerLevelBridge;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Snow, iron and copper golems built from pumpkins report their BUILD_* spawn reason and honour a cancelled spawn. */
@Mixin(CarvedPumpkinBlock.class)
public abstract class GolemBuildSpawnMixin {
    @Inject(method = "spawnGolemInWorld", at = @At("HEAD"), cancellable = true)
    private static void lunararc$spawnGolem(Level level, BlockPattern.BlockPatternMatch match, Entity golem, BlockPos pos, CallbackInfo ci) {
        ci.cancel();
        golem.snapTo(pos.getX() + 0.5, pos.getY() + 0.05, pos.getZ() + 0.5, 0.0F, 0.0F);
        CreatureSpawnEvent.SpawnReason reason;
        if (golem.getType() == net.minecraft.world.entity.EntityTypes.SNOW_GOLEM) reason = CreatureSpawnEvent.SpawnReason.BUILD_SNOWMAN;
        else if (golem.getType() == net.minecraft.world.entity.EntityTypes.COPPER_GOLEM) reason = CreatureSpawnEvent.SpawnReason.BUILD_COPPERGOLEM;
        else reason = CreatureSpawnEvent.SpawnReason.BUILD_IRONGOLEM;
        if (!((ServerLevelBridge) level).lunararc$addFreshEntity(golem, reason)) return;
        CarvedPumpkinBlock.clearPatternBlocks(level, match);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, golem.getBoundingBox().inflate(5.0))) {
            CriteriaTriggers.SUMMONED_ENTITY.trigger(player, golem);
        }
        CarvedPumpkinBlock.updatePatternBlocks(level, match);
    }

    @WrapWithCondition(method = "trySpawnGolem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/CarvedPumpkinBlock;replaceCopperBlockWithChest(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/pattern/BlockPattern$BlockPatternMatch;)V"))
    private boolean lunararc$chestOnlyWhenSpawned(CarvedPumpkinBlock block, Level level, BlockPattern.BlockPatternMatch match,
            @com.llamalad7.mixinextras.sugar.Local CopperGolem golem) {
        return lunararc$inWorld(level, golem);
    }

    @WrapWithCondition(method = "trySpawnGolem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/animal/golem/CopperGolem;spawn(Lnet/minecraft/world/level/block/WeatheringCopper$WeatherState;)V"))
    private boolean lunararc$weatherOnlyWhenSpawned(CopperGolem golem, net.minecraft.world.level.block.WeatheringCopper.WeatherState state) {
        return lunararc$inWorld(golem.level(), golem);
    }

    @org.spongepowered.asm.mixin.Unique
    private static boolean lunararc$inWorld(Level level, Entity golem) {
        return level instanceof ServerLevel server && server.getEntity(golem.getUUID()) != null;
    }
}
