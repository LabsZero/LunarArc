package io.lunararcdevs.lunararc.forge.mixin.core.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShearsItem.class)
public abstract class ShearsItemMixin_Forge {
    @Inject(method = "interactLivingEntity", cancellable = true, at = @At(value = "INVOKE", remap = false,
            target = "Lnet/minecraftforge/common/IForgeShearable;onSheared(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;I)Ljava/util/List;"))
    private void lunararc$shear(ItemStack stack, Player player, LivingEntity entity, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (CraftEventFactory.callPlayerShearEntityCancelled(player, entity, stack, hand)) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
