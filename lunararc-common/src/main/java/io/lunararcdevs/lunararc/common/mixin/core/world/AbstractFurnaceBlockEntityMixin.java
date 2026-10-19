package io.lunararcdevs.lunararc.common.mixin.core.world;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.inventory.CraftRecipeAdapter;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceBlockEntityMixin implements io.lunararcdevs.lunararc.common.bridge.world.CookSpeedBridge {

    @Shadow public int cookingTimer;
    @Shadow int cookingTotalTime;

    @Shadow public int litTimeRemaining;


    @Unique private static int shadowTotalCookTime(ServerLevel level, AbstractFurnaceBlockEntity furnace) { return AbstractFurnaceBlockEntity.getTotalCookTime(level, furnace); }

    @Override public boolean lunararc$isLit() { return this.litTimeRemaining > 0; }
    @Override public int lunararc$cookingProgress() { return this.cookingTimer; }
    @Override public int lunararc$cookingTotalTime() { return this.cookingTotalTime; }
    @Override public void lunararc$setCookingTotalTime(int ticks) { this.cookingTotalTime = ticks; }

    public double cookSpeedMultiplier = 1.0D;
    @Unique private net.minecraft.world.item.crafting.RecipeType<? extends net.minecraft.world.item.crafting.AbstractCookingRecipe> lunararc$recipeType;

    @Override
    public net.minecraft.world.item.crafting.RecipeType<? extends net.minecraft.world.item.crafting.AbstractCookingRecipe> lunararc$recipeType() {
        return this.lunararc$recipeType;
    }

    @Override
    public double lunararc$cookSpeed() {
        return this.cookSpeedMultiplier;
    }


    @Inject(method = "<init>", at = @At("TAIL"))
    private void lunararc$captureRecipeType(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos,
            BlockState state, net.minecraft.world.item.crafting.RecipeType<? extends net.minecraft.world.item.crafting.AbstractCookingRecipe> recipeType,
            CallbackInfo ci) {
        this.lunararc$recipeType = recipeType;
    }

    @ModifyReturnValue(method = "getTotalCookTime(Lnet/minecraft/world/item/crafting/RecipeHolder;Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;)I", at = @At("RETURN"))
    private static int lunararc$applyCookSpeed(int original, RecipeHolder<?> holder, AbstractFurnaceBlockEntity furnace) {
        double multiplier = ((io.lunararcdevs.lunararc.common.bridge.world.CookSpeedBridge) furnace).lunararc$cookSpeed();
        return multiplier == 1.0D ? original : (int) Math.ceil(original / multiplier);
    }

    @Inject(method = "serverTick", at = @At("HEAD"))
    private static void lunararc$captureFurnace(ServerLevel level, BlockPos pos, BlockState state, AbstractFurnaceBlockEntity furnace, CallbackInfo ci) {
        io.lunararcdevs.lunararc.common.event.LunarArcFurnaceState.level = org.bukkit.Bukkit.isPrimaryThread() ? level : null;
        io.lunararcdevs.lunararc.common.event.LunarArcFurnaceState.pos = pos;
        io.lunararcdevs.lunararc.common.event.LunarArcFurnaceState.burnEvent = null;
    }

    @WrapOperation(method = "serverTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;getBurnDuration(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;)I"))
    private static int lunararc$furnaceBurn(AbstractFurnaceBlockEntity furnace, ServerLevel level, ItemStack fuel, Operation<Integer> original) {
        int duration = original.call(furnace, level, fuel);
        if (io.lunararcdevs.lunararc.common.event.LunarArcFurnaceState.level == null) return duration;
        FurnaceBurnEvent event = new FurnaceBurnEvent(CraftBlock.at(io.lunararcdevs.lunararc.common.event.LunarArcFurnaceState.level, io.lunararcdevs.lunararc.common.event.LunarArcFurnaceState.pos), CraftItemStack.asCraftMirror(fuel), duration);
        org.bukkit.Bukkit.getPluginManager().callEvent(event);
        io.lunararcdevs.lunararc.common.event.LunarArcFurnaceState.burnEvent = event;
        return event.isCancelled() ? 0 : event.getBurnTime();
    }

    @WrapOperation(method = "consumeFuel", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"))
    private static void lunararc$consumeFuel(ItemStack fuel, int amount, Operation<Void> original) {
        FurnaceBurnEvent event = io.lunararcdevs.lunararc.common.event.LunarArcFurnaceState.burnEvent;
        if (event != null && (!event.isBurning() || !event.willConsumeFuel())) return;
        original.call(fuel, amount);
    }

    @com.llamalad7.mixinextras.injector.ModifyExpressionValue(method = "serverTick", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;canBurn(Lnet/minecraft/core/NonNullList;ILnet/minecraft/world/item/ItemStack;)Z"))
    private static boolean lunararc$startSmelt(boolean can, @Local(argsOnly = true) AbstractFurnaceBlockEntity furnace,
            @Local(argsOnly = true) ServerLevel level, @Local(argsOnly = true) BlockPos pos) {
        io.lunararcdevs.lunararc.common.bridge.world.CookSpeedBridge bridge = (io.lunararcdevs.lunararc.common.bridge.world.CookSpeedBridge) furnace;
        if (can && bridge.lunararc$isLit() && bridge.lunararc$cookingProgress() == 0) {
            bridge.lunararc$setCookingTotalTime(io.lunararcdevs.lunararc.common.event.LunarArcPaperEvents.furnaceStart(
                    level, pos, furnace.getItem(0), bridge.lunararc$recipeType(),
                    bridge.lunararc$cookingTotalTime() > 0 ? bridge.lunararc$cookingTotalTime() : shadowTotalCookTime(level, furnace)));
        }
        return can;
    }
}
