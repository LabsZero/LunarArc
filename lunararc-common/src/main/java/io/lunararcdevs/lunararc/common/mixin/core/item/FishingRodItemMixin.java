package io.lunararcdevs.lunararc.common.mixin.core.item;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.lunararcdevs.lunararc.common.bridge.EntityBridge;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FishingRodItem.class)
public abstract class FishingRodItemMixin {
    @WrapOperation(method = "use", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean lunararc$cast(Level level, Entity entity, Operation<Boolean> original, Level unused, Player player, InteractionHand hand) {
        if (entity instanceof FishingHook hook
                && ((EntityBridge) player).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Player bukkitPlayer
                && ((EntityBridge) hook).lunararc$getBukkitEntity() instanceof org.bukkit.entity.FishHook bukkitHook) {
            var event = new org.bukkit.event.player.PlayerFishEvent(bukkitPlayer, null, bukkitHook,
                    org.bukkit.craftbukkit.CraftEquipmentSlot.getHand(hand), org.bukkit.event.player.PlayerFishEvent.State.FISHING);
            org.bukkit.Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                player.fishing = null;
                return false;
            }
        }
        return original.call(level, entity);
    }
}
