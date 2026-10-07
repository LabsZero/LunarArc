package io.lunararcdevs.lunararc.common.mixin.core.item;

import io.lunararcdevs.lunararc.common.event.LunarArcPaperEvents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DyeItem.class)
public abstract class DyeItemMixin {

    @Shadow @Final private DyeColor dyeColor;

    @Inject(method = "interactLivingEntity", at = @At("HEAD"), cancellable = true, require = 0)
    private void lunararc$dye(ItemStack stack, Player player, LivingEntity target, InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir) {
        if (!(target instanceof Sheep sheep) || !sheep.isAlive() || sheep.isSheared() || sheep.getColor() == this.dyeColor
                || player.level().isClientSide) return;
        DyeColor color = LunarArcPaperEvents.sheepDye(player, sheep, this.dyeColor);
        if (color == null) {
            cir.setReturnValue(InteractionResult.PASS);
        } else if (color != this.dyeColor) {
            sheep.level().playSound(player, sheep, SoundEvents.DYE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
            sheep.setColor(color);
            stack.shrink(1);
            cir.setReturnValue(InteractionResult.sidedSuccess(false));
        }
    }
}
