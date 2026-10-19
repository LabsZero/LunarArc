package io.lunararcdevs.lunararc.neoforge.mixin.core.entity;

import io.lunararcdevs.lunararc.common.bridge.entity.SlimeBridge;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.entity.monster.cubemob.AbstractCubeMob$CubeMobKeepOnJumpingGoal", remap = false)
public abstract class SlimeKeepOnJumpingGoalMixin_NeoForge {
    @Shadow(remap = false) @Final private AbstractCubeMob cubeMob;

    @Inject(method = "canUse", remap = false, at = @At("HEAD"), cancellable = true)
    private void lunararc$paperCanWander(CallbackInfoReturnable<Boolean> cir) {
        if (this.cubeMob instanceof SlimeBridge bridge && !bridge.lunararc$canWander()) cir.setReturnValue(false);
    }

    // SlimeWanderEvent: a plugin may stop a slime from picking its jumping goal.
    @Inject(method = "canUse", remap = false, at = @At("RETURN"), cancellable = true)
    private void lunararc$wanderEvent(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && org.bukkit.Bukkit.isPrimaryThread()
                && com.destroystokyo.paper.event.entity.SlimeWanderEvent.getHandlerList().getRegisteredListeners().length != 0
                && ((io.lunararcdevs.lunararc.common.bridge.EntityBridge) (Object) this.cubeMob).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Slime bukkit
                && !new com.destroystokyo.paper.event.entity.SlimeWanderEvent(bukkit).callEvent()) {
            cir.setReturnValue(false);
        }
    }
}
