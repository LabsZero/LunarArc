package io.lunararcdevs.lunararc.common.mixin.core.world.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.lunararcdevs.lunararc.common.event.LunarArcMoreEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EndDragonFight.class)
public abstract class EndDragonEggMixin {

    @WrapOperation(method = "setDragonKilled", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean lunararc$egg(ServerLevel level, BlockPos pos, BlockState state, Operation<Boolean> original) {
        if (state.is(Blocks.DRAGON_EGG) && !LunarArcMoreEvents.dragonEgg(level, pos, state, (EndDragonFight) (Object) this)) return false;
        return original.call(level, pos, state);
    }
}
