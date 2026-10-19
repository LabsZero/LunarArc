package io.lunararcdevs.lunararc.common.mixin.core.events;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.lunararcdevs.lunararc.common.bridge.EntityBridge;
import io.papermc.paper.event.entity.EntityDyeEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Paper's EntityDyeEvent for wolf and cat collars. A cancelled dye reads as "already that colour", so nothing is consumed. */
@Mixin({Wolf.class, Cat.class})
public abstract class CollarDyeEventMixin {
    @WrapOperation(method = "mobInteract", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;get(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;"))
    private Object lunararc$dye(net.minecraft.world.item.ItemStack stack, net.minecraft.core.component.DataComponentType<?> type,
            Operation<Object> original, Player player, InteractionHand hand) {
        Object value = original.call(stack, type);
        if (type != net.minecraft.core.component.DataComponents.DYE || !(value instanceof DyeColor requested)) return value;
        Object self = this;
        DyeColor current = self instanceof Wolf wolf ? wolf.getCollarColor() : ((Cat) self).getCollarColor();
        org.bukkit.entity.Entity bukkit = ((EntityBridge) this).lunararc$getBukkitEntity();
        if (!(((EntityBridge) player).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Player bukkitPlayer)) return requested;
        EntityDyeEvent event = new EntityDyeEvent(bukkit, org.bukkit.DyeColor.getByWoolData((byte) requested.getId()), bukkitPlayer);
        if (!event.callEvent()) return current;
        return DyeColor.byId(event.getColor().getWoolData());
    }
}
