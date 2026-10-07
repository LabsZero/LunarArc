package io.lunararcdevs.lunararc.common.mixin.core.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.lunararcdevs.lunararc.common.event.LunarArcMoreEvents;
import io.papermc.paper.event.player.PlayerItemFrameChangeEvent.ItemFrameChangeAction;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemFrame.class)
public abstract class ItemFrameEventsMixin {

    @WrapOperation(method = "interact", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/decoration/ItemFrame;setItem(Lnet/minecraft/world/item/ItemStack;)V"))
    private void lunararc$place(ItemFrame frame, ItemStack stack, Operation<Void> original, @Local(argsOnly = true) Player player) {
        if (LunarArcMoreEvents.itemFrame(player, frame, stack, ItemFrameChangeAction.PLACE)) original.call(frame, stack);
    }

    @WrapOperation(method = "interact", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/decoration/ItemFrame;setRotation(I)V"))
    private void lunararc$rotate(ItemFrame frame, int rotation, Operation<Void> original, @Local(argsOnly = true) Player player) {
        if (LunarArcMoreEvents.itemFrame(player, frame, frame.getItem(), ItemFrameChangeAction.ROTATE)) original.call(frame, rotation);
    }
}
