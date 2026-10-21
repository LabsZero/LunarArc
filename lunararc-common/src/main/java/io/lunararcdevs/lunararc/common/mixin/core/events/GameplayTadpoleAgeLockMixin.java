package io.lunararcdevs.lunararc.common.mixin.core.events;

import io.lunararcdevs.lunararc.common.event.LunarArcGameplayEvents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.frog.Tadpole;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** PlayerToggleEntityAgeLockEvent for tadpoles, which lock their age through their own copy of the logic. */
@Mixin(Tadpole.class)
public abstract class GameplayTadpoleAgeLockMixin {
    @Shadow @Final private static EntityDataAccessor<Boolean> AGE_LOCKED;

    @Inject(method = "mobInteract", cancellable = true, require = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/AgeableMob;setAgeLocked(Lnet/minecraft/world/entity/Mob;Ljava/util/function/Supplier;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;Ljava/util/function/Consumer;)V"))
    private void lunararc$ageLock(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        Tadpole self = (Tadpole) (Object) this;
        if (!LunarArcGameplayEvents.toggleAgeLock(player, self, player.getItemInHand(hand), hand, !self.isAgeLocked(), AGE_LOCKED)) {
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }
}
