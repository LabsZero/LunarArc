package io.lunararcdevs.lunararc.neoforge.mixin.core.entity;

import io.lunararcdevs.lunararc.common.bridge.entity.SlimeBridge;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.entity.monster.cubemob.AbstractCubeMob$CubeMobRandomDirectionGoal", remap = false)
public abstract class SlimeRandomDirectionGoalMixin_NeoForge {
    @Shadow(remap = false) @Final private AbstractCubeMob cubeMob;

    @Inject(method = "canUse", remap = false, at = @At("HEAD"), cancellable = true)
    private void lunararc$paperCanWander(CallbackInfoReturnable<Boolean> cir) {
        if (this.cubeMob instanceof SlimeBridge bridge && !bridge.lunararc$canWander()) cir.setReturnValue(false);
    }

    // SlimeChangeDirectionEvent: a plugin may change the new heading, or keep the previous one by cancelling.
    @WrapOperation(method = "tick", remap = false, require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(I)I", ordinal = 1, remap = false))
    private int lunararc$changeDirectionEvent(net.minecraft.util.RandomSource random, int bound, Operation<Integer> original) {
        int degrees = original.call(random, bound);
        if (!org.bukkit.Bukkit.isPrimaryThread()
                || com.destroystokyo.paper.event.entity.SlimeChangeDirectionEvent.getHandlerList().getRegisteredListeners().length == 0
                || !(((io.lunararcdevs.lunararc.common.bridge.EntityBridge) (Object) this.cubeMob).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Slime bukkit)) {
            return degrees;
        }
        com.destroystokyo.paper.event.entity.SlimeChangeDirectionEvent event =
                new com.destroystokyo.paper.event.entity.SlimeChangeDirectionEvent(bukkit, degrees);
        if (!event.callEvent()) return degrees;
        return Math.round(event.getNewYaw());
    }
}
