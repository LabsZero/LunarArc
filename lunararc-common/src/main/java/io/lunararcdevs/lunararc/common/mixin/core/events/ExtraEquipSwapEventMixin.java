package io.lunararcdevs.lunararc.common.mixin.core.events;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.lunararcdevs.lunararc.common.event.LunarArcExtraEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.equipment.Equippable;
import org.spongepowered.asm.mixin.Mixin;

/** PlayerSwapWithEquipmentSlotEvent: a plugin may stop a right-click armor swap. */
@Mixin(Equippable.class)
public abstract class ExtraEquipSwapEventMixin {
    @WrapMethod(method = "swapWithEquipmentSlot", require = 0)
    private InteractionResult lunararc$swap(ItemStack stack, Player player, Operation<InteractionResult> original) {
        Equippable self = (Equippable) (Object) this;
        if (player.canUseSlot(self.slot()) && self.canBeEquippedBy(player.getType().builtInRegistryHolder())) {
            ItemStack current = player.getItemBySlot(self.slot());
            boolean allowedByEnchant = !EnchantmentHelper.has(current, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE) || player.isCreative();
            if (allowedByEnchant && !ItemStack.isSameItemSameComponents(stack, current)
                    && LunarArcExtraEvents.equipSwapCancelled(player, stack, self.slot(), current)) {
                return InteractionResult.FAIL;
            }
        }
        return original.call(stack, player);
    }
}
