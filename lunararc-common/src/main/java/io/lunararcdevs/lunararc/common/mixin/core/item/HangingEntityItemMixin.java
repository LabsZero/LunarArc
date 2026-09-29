package io.lunararcdevs.lunararc.common.mixin.core.item;

import com.llamalad7.mixinextras.sugar.Local;
import io.lunararcdevs.lunararc.common.bridge.EntityBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.item.HangingEntityItem;
import net.minecraft.world.item.context.UseOnContext;
import org.bukkit.craftbukkit.CraftEquipmentSlot;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HangingEntityItem.class)
public abstract class HangingEntityItemMixin {
    @Inject(method = "useOn", cancellable = true, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/decoration/HangingEntity;playPlacementSound()V"))
    private void lunararc$hangingPlace(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir,
            @Local HangingEntity hanging) {
        if (!(context.getLevel() instanceof ServerLevel level)
                || !(((EntityBridge) hanging).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Hanging bukkitHanging)) {
            return;
        }
        var player = context.getPlayer() == null ? null : ((EntityBridge) context.getPlayer()).lunararc$getBukkitEntity();
        BlockPos pos = context.getClickedPos();
        HangingPlaceEvent event = new HangingPlaceEvent(bukkitHanging,
                player instanceof org.bukkit.entity.Player p ? p : null, CraftBlock.at(level, pos),
                CraftBlock.notchToBlockFace(context.getClickedFace()), CraftEquipmentSlot.getHand(context.getHand()),
                CraftItemStack.asBukkitCopy(context.getItemInHand()));
        org.bukkit.Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
