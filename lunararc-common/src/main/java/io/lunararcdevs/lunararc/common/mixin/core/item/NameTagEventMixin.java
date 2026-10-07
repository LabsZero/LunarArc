package io.lunararcdevs.lunararc.common.mixin.core.item;

import io.lunararcdevs.lunararc.common.event.LunarArcMoreEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.NameTagItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NameTagItem.class)
public abstract class NameTagEventMixin {

    @Inject(method = "interactLivingEntity", at = @At("HEAD"), cancellable = true, require = 0)
    private void lunararc$name(ItemStack stack, Player player, LivingEntity target, InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir) {
        if (stack.get(DataComponents.CUSTOM_NAME) == null || target instanceof Player || player.level().isClientSide || !target.isAlive()) return;
        if (!LunarArcMoreEvents.nameEntity(player, target, stack)) cir.setReturnValue(InteractionResult.PASS);
    }
}
