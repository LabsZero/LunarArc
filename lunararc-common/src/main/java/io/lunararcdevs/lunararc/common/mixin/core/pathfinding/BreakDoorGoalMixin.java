package io.lunararcdevs.lunararc.common.mixin.core.pathfinding;

import io.lunararcdevs.lunararc.common.bridge.EntityBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.BreakDoorGoal;
import net.minecraft.world.level.block.Blocks;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BreakDoorGoal.class)
public abstract class BreakDoorGoalMixin {
    @Inject(method = "tick", cancellable = true, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"))
    private void lunararc$breakDoor(CallbackInfo ci) {
        Mob mob = ((DoorInteractGoalAccessor) this).lunararc$getMob();
        BlockPos door = ((DoorInteractGoalAccessor) this).lunararc$getDoorPos();
        if (!(mob.level() instanceof ServerLevel level)
                || !(((EntityBridge) mob).lunararc$getBukkitEntity() instanceof org.bukkit.entity.LivingEntity living)) {
            return;
        }
        var event = new org.bukkit.event.entity.EntityBreakDoorEvent(living, CraftBlock.at(level, door),
                CraftBlockData.fromData(Blocks.AIR.defaultBlockState()));
        org.bukkit.Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            ((BreakDoorGoal) (Object) this).start();
            ci.cancel();
        }
    }
}
