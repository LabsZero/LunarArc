package io.lunararcdevs.lunararc.common.mixin.core.events;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.lunararcdevs.lunararc.common.event.LunarArcBlockEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.CrafterBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** CrafterCraftEvent: a plugin may cancel a crafter's craft or change its result. */
@Mixin(CrafterBlock.class)
public abstract class ExtraCrafterEventMixin {
    @WrapOperation(method = "dispenseFrom", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/crafting/CraftingRecipe;assemble(Lnet/minecraft/world/item/crafting/RecipeInput;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack lunararc$craft(CraftingRecipe recipe, net.minecraft.world.item.crafting.RecipeInput input, Operation<ItemStack> original,
            @Local(argsOnly = true) ServerLevel level, @Local(argsOnly = true) BlockPos pos, @Local RecipeHolder<CraftingRecipe> holder) {
        ItemStack result = original.call(recipe, input);
        ItemStack changed = LunarArcBlockEvents.crafterCraft(level, pos, result, holder);
        return changed == null ? ItemStack.EMPTY : changed;
    }
}
