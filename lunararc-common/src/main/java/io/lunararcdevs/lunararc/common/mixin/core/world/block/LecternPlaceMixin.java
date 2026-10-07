package io.lunararcdevs.lunararc.common.mixin.core.world.block;

import io.lunararcdevs.lunararc.common.event.LunarArcMoreEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LecternBlock.class)
public abstract class LecternPlaceMixin {

    @Inject(method = "placeBook", at = @At("HEAD"), cancellable = true, require = 0)
    private static void lunararc$insert(LivingEntity who, Level level, BlockPos pos, BlockState state, ItemStack book, CallbackInfo ci) {
        if (!LunarArcMoreEvents.lecternInsert(who, level, pos, book)) ci.cancel();
    }
}
