package io.lunararcdevs.lunararc.common.mixin.core.entity;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import io.lunararcdevs.lunararc.common.bridge.EntityBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(Bucketable.class)
public interface BucketableMixin {
    @Inject(method = "bucketMobPickup", cancellable = true, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemUtils;createFilledResult(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/item/ItemStack;"))
    private static <T extends LivingEntity & Bucketable> void lunararc$bucketEntity(Player player, InteractionHand hand, T entity,
            CallbackInfoReturnable<Optional<InteractionResult>> cir, @Local(ordinal = 0) ItemStack held, @Local(ordinal = 1) LocalRef<ItemStack> bucket) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        var event = CraftEventFactory.callPlayerFishBucketEvent(entity, player, held, bucket.get(), hand);
        if (event.isCancelled()) {
            serverPlayer.containerMenu.sendAllDataToRemote();
            if (((EntityBridge) entity).lunararc$getBukkitEntity() instanceof org.bukkit.craftbukkit.entity.CraftEntity craft) {
                craft.update(serverPlayer);
            }
            cir.setReturnValue(Optional.of(InteractionResult.FAIL));
            return;
        }
        bucket.set(CraftItemStack.asNMSCopy(event.getEntityBucket()));
    }
}
