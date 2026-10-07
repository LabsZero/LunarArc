package io.lunararcdevs.lunararc.common.mixin.core.world.blockentity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(CampfireBlockEntity.class)
public abstract class CampfireBlockEntityMixin implements io.lunararcdevs.lunararc.common.bridge.world.StopCookingBridge {
    public final boolean[] stopCooking = new boolean[4];

    @Override
    public boolean[] lunararc$stopCooking() {
        return this.stopCooking;
    }

    @WrapOperation(method = "cookTick", at = @At(value = "INVOKE", ordinal = 0,
            target = "Lnet/minecraft/core/NonNullList;get(I)Ljava/lang/Object;"))
    private static Object lunararc$skipStopped(NonNullList<ItemStack> items, int slot, Operation<Object> original,
            net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos,
            net.minecraft.world.level.block.state.BlockState state, CampfireBlockEntity campfire) {
        boolean[] stopped = ((io.lunararcdevs.lunararc.common.bridge.world.StopCookingBridge) campfire).lunararc$stopCooking();
        return slot < stopped.length && stopped[slot] ? ItemStack.EMPTY : original.call(items, slot);
    }

    @Shadow @org.spongepowered.asm.mixin.Final private NonNullList<ItemStack> items;

    @Shadow public abstract java.util.Optional<net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.CampfireCookingRecipe>> getCookableRecipe(ItemStack stack);

    @ModifyVariable(method = "placeFood", at = @At("HEAD"), argsOnly = true, require = 0)
    private int lunararc$startCooking(int cookTime, net.minecraft.world.entity.LivingEntity entity, ItemStack food, int original) {
        CampfireBlockEntity self = (CampfireBlockEntity) (Object) this;
        if (self.getLevel() == null || !this.items.stream().anyMatch(ItemStack::isEmpty)) return cookTime;
        return io.lunararcdevs.lunararc.common.event.LunarArcPaperEvents.campfireStart(
                self.getLevel(), self.getBlockPos(), food, this.getCookableRecipe(food).orElse(null), cookTime);
    }
}
