package io.lunararcdevs.lunararc.common.mixin.core.item;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.lunararcdevs.lunararc.common.bridge.EntityBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.LeadItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Bukkit leash-to-fence cancellation around the real LeadItem leash assignment. */
@Mixin(LeadItem.class)
public abstract class LeadItemMixin {
    @Unique private static final ThreadLocal<InteractionHand> lunararc$hand =
            ThreadLocal.withInitial(() -> InteractionHand.MAIN_HAND);

    @Inject(method = "useOn", at = @At("HEAD"), require = 0)
    private void lunararc$captureHand(UseOnContext context, CallbackInfoReturnable<net.minecraft.world.InteractionResult> cir) {
        lunararc$hand.set(context.getHand());
    }

    @Inject(method = "bindPlayerMobs", cancellable = true, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/decoration/LeashFenceKnotEntity;playPlacementSound()V"))
    private static void lunararc$knotPlace(Player player, Level level, BlockPos pos,
            CallbackInfoReturnable<net.minecraft.world.InteractionResult> cir, @Local LeashFenceKnotEntity knot) {
        var bukkitKnot = ((EntityBridge) knot).lunararc$getBukkitEntity();
        var bukkitPlayer = player == null ? null : ((EntityBridge) player).lunararc$getBukkitEntity();
        if (!(bukkitKnot instanceof org.bukkit.entity.Hanging hanging) || !(level instanceof ServerLevel serverLevel)) return;
        var event = new org.bukkit.event.hanging.HangingPlaceEvent(hanging,
                bukkitPlayer instanceof org.bukkit.entity.Player p ? p : null,
                org.bukkit.craftbukkit.block.CraftBlock.at(serverLevel, pos), org.bukkit.block.BlockFace.SELF,
                org.bukkit.craftbukkit.CraftEquipmentSlot.getHand(lunararc$hand.get()));
        org.bukkit.Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            knot.discard();
            cir.setReturnValue(net.minecraft.world.InteractionResult.PASS);
        }
    }

    @WrapOperation(
            method = "bindPlayerMobs",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Leashable;setLeashedTo(Lnet/minecraft/world/entity/Entity;Z)V"),
            require = 0)
    private static void lunararc$leashEvent(
            Leashable leashable, Entity holder, boolean broadcast, Operation<Void> original,
            Player player, Level level, BlockPos pos) {
        if (leashable instanceof Entity entity) {
            var event = org.bukkit.craftbukkit.event.CraftEventFactory.callPlayerLeashEntityEvent(
                    entity, holder, player, lunararc$hand.get());
            if (event.isCancelled()) {
                return;
            }
        }
        original.call(leashable, holder, broadcast);
    }
}
