package io.lunararcdevs.lunararc.common.mixin.core.pathfinding;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.DoorInteractGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DoorInteractGoal.class)
public interface DoorInteractGoalAccessor {
    @Accessor("mob") Mob lunararc$getMob();
    @Accessor("doorPos") BlockPos lunararc$getDoorPos();
}
