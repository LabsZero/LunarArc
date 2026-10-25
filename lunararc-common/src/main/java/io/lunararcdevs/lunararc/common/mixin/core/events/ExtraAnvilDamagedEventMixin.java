package io.lunararcdevs.lunararc.common.mixin.core.events;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.lunararcdevs.lunararc.common.event.LunarArcBlockEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** AnvilDamagedEvent: a plugin may cancel, change or force-break the damage an anvil takes when used. */
@Mixin(AnvilMenu.class)
public abstract class ExtraAnvilDamagedEventMixin {
    @WrapOperation(method = {"lambda$onTake$0", "lambda$onTake$1", "lambda$onTake$2", "lambda$onTake$3"}, remap = false, require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/AnvilBlock;damage(Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private static @Nullable BlockState lunararc$damage(BlockState state, Operation<BlockState> original, @Local Player player) {
        BlockState damaged = original.call(state);
        return LunarArcBlockEvents.anvilDamaged(player, state, damaged);
    }
}
