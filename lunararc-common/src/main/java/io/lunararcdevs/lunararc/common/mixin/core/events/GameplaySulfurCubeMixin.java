package io.lunararcdevs.lunararc.common.mixin.core.events;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import io.lunararcdevs.lunararc.common.event.LunarArcGameplayEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.cubemob.SulfurCube;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** SulfurCubeSwallowItemEvent and EntityIgniteEvent for the 26.x sulfur cube. */
@Mixin(SulfurCube.class)
public abstract class GameplaySulfurCubeMixin {
    @WrapOperation(method = "mobInteract", require = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/cubemob/SulfurCube;equipItem(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean lunararc$swallow(SulfurCube cube, ItemStack held, Operation<Boolean> original,
            @Local(argsOnly = true) Player player, @Local(argsOnly = true) InteractionHand hand) {
        ItemStack replacement = LunarArcGameplayEvents.swallow(cube, player, held, hand);
        if (replacement == null) return false;
        boolean unchanged = replacement == held;
        boolean worked = original.call(cube, replacement);
        // vanilla consumes one held item after a successful swallow; a plugin that swapped the item keeps the stack whole
        if (worked && !unchanged && !player.hasInfiniteMaterials()) held.grow(1);
        return worked;
    }

    @Inject(method = "primeTime", cancellable = true, require = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/cubemob/SulfurCube;setPermanentlyInvulnerable(Z)V"))
    private void lunararc$ignite(boolean imminent, CallbackInfoReturnable<Boolean> cir, @Local(ordinal = 1) LocalIntRef fuseTime) {
        int adjusted = LunarArcGameplayEvents.ignite((SulfurCube) (Object) this, fuseTime.get());
        if (adjusted == PrimedTnt.NO_FUSE) {
            cir.setReturnValue(false);
            return;
        }
        fuseTime.set(adjusted);
    }
}
