package io.lunararcdevs.lunararc.common.mixin.core.entity;

import io.lunararcdevs.lunararc.common.bridge.EntityBridge;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Witch.class)
public abstract class WitchMixin {
    @Shadow private int usingTime;
    @Shadow @Final private static ResourceLocation SPEED_MODIFIER_DRINKING_ID;
    @Shadow @Final private static AttributeModifier SPEED_MODIFIER_DRINKING;

    public void setDrinkingPotion(ItemStack potion) {
        Witch self = (Witch) (Object) this;
        if (self.level() instanceof net.minecraft.server.level.ServerLevel
                && ((EntityBridge) self).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Witch witch) {
            var event = new com.destroystokyo.paper.event.entity.WitchReadyPotionEvent(witch, CraftItemStack.asCraftMirror(potion));
            potion = event.callEvent() ? CraftItemStack.asNMSCopy(event.getPotion()) : ItemStack.EMPTY;
        }
        self.setItemSlot(EquipmentSlot.MAINHAND, potion);
        this.usingTime = self.getMainHandItem().getUseDuration(self);
        self.setUsingItem(true);
        if (!self.isSilent()) {
            self.level().playSound(null, self.getX(), self.getY(), self.getZ(), SoundEvents.WITCH_DRINK,
                    self.getSoundSource(), 1.0F, 0.8F + self.getRandom().nextFloat() * 0.4F);
        }
        var speed = self.getAttribute(Attributes.MOVEMENT_SPEED);
        speed.removeModifier(SPEED_MODIFIER_DRINKING_ID);
        speed.addTransientModifier(SPEED_MODIFIER_DRINKING);
    }
}
