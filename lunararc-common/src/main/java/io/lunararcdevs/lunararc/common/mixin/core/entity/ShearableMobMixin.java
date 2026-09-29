package io.lunararcdevs.lunararc.common.mixin.core.entity;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.animal.SnowGolem;
import net.minecraft.world.entity.monster.Bogged;
import net.minecraft.world.entity.player.Player;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Sheep.class, MushroomCow.class, SnowGolem.class, Bogged.class})
public abstract class ShearableMobMixin {
    @Inject(method = "mobInteract", cancellable = true, require = 0, at = {
            @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/Sheep;shear(Lnet/minecraft/sounds/SoundSource;)V"),
            @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/MushroomCow;shear(Lnet/minecraft/sounds/SoundSource;)V"),
            @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/SnowGolem;shear(Lnet/minecraft/sounds/SoundSource;)V"),
            @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/Bogged;shear(Lnet/minecraft/sounds/SoundSource;)V")})
    private void lunararc$shear(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (CraftEventFactory.callPlayerShearEntityCancelled(player, (Entity) (Object) this, player.getItemInHand(hand), hand)) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
