package io.lunararcdevs.lunararc.common.mixin.core.entity;

import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Guardian;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Guardian.class)
public abstract class GuardianMixin {
    public Guardian.GuardianAttackGoal guardianAttackGoal;

    @ModifyArg(method = "registerGoals", index = 1, at = @At(value = "INVOKE", ordinal = 2,
            target = "Lnet/minecraft/world/entity/ai/goal/GoalSelector;addGoal(ILnet/minecraft/world/entity/ai/goal/Goal;)V"))
    private Goal lunararc$captureAttackGoal(Goal goal) {
        if (goal instanceof Guardian.GuardianAttackGoal attackGoal) {
            this.guardianAttackGoal = attackGoal;
        }
        return goal;
    }
}
