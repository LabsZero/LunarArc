package io.lunararcdevs.lunararc.common.mixin.core.world.block;

import io.lunararcdevs.lunararc.common.event.LunarArcMoreEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NoteBlock.class)
public abstract class NoteBlockEventMixin {

    @Inject(method = "triggerEvent", at = @At("HEAD"), cancellable = true, require = 0)
    private void lunararc$note(BlockState state, Level level, BlockPos pos, int id, int param, CallbackInfoReturnable<Boolean> cir) {
        if (!LunarArcMoreEvents.notePlay(level, pos, state)) cir.setReturnValue(false);
    }
}
