package io.lunararcdevs.lunararc.common.mixin.core.entity;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ItemFrame.class)
public abstract class ItemFrameMixin {
    @Shadow @Final private static EntityDataAccessor<ItemStack> DATA_ITEM;
    @Shadow private void onItemChanged(ItemStack stack) { throw new AssertionError(); }
    @Shadow public abstract SoundEvent getAddItemSound();

    public void setItem(ItemStack stack, boolean updateNeighbours, boolean playSound) {
        ItemFrame self = (ItemFrame) (Object) this;
        if (!stack.isEmpty()) {
            stack = stack.copyWithCount(1);
        }
        this.onItemChanged(stack);
        self.getEntityData().set(DATA_ITEM, stack);
        if (!stack.isEmpty() && playSound) {
            self.playSound(this.getAddItemSound(), 1.0F, 1.0F);
        }
        if (updateNeighbours && self.getPos() != null) {
            self.level().updateNeighbourForOutputSignal(self.getPos(), Blocks.AIR);
        }
    }
}
