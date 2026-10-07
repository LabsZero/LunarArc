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
    @Shadow @Final private it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap<net.minecraft.resources.ResourceLocation> recipesUsed;

    @Shadow int cookingProgress;
    @Shadow int cookingTotalTime;

    @Shadow protected abstract boolean isLit();

    @Shadow private static int getTotalCookTime(Level level, AbstractFurnaceBlockEntity furnace) { throw new AssertionError(); }

    @Unique private static int shadowTotalCookTime(Level level, AbstractFurnaceBlockEntity furnace) { return getTotalCookTime(level, furnace); }

    @Override public boolean lunararc$isLit() { return this.isLit(); }
    @Override public int lunararc$cookingProgress() { return this.cookingProgress; }
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

    public it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap<net.minecraft.resources.ResourceLocation> getRecipesUsed() {
        return this.recipesUsed;
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void lunararc$captureRecipeType(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos,
            BlockState state, net.minecraft.world.item.crafting.RecipeType<? extends net.minecraft.world.item.crafting.AbstractCookingRecipe> recipeType,
            CallbackInfo ci) {
        this.lunararc$recipeType = recipeType;
    }

    @ModifyReturnValue(method = "getTotalCookTime", at = @At("RETURN"))
    private static int lunararc$applyCookSpeed(int original, Level level, AbstractFurnaceBlockEntity furnace) {
        double multiplier = ((io.lunararcdevs.lunararc.common.bridge.world.CookSpeedBridge) furnace).lunararc$cookSpeed();
        return multiplier == 1.0D ? original : (int) Math.ceil(original / multiplier);
    }

    @Unique private static ServerLevel lunararc$tickLevel;
    @Unique private static BlockPos lunararc$tickPos;
    @Unique private static FurnaceBurnEvent lunararc$burnEvent;

    @Inject(method = "serverTick", at = @At("HEAD"))
    private static void lunararc$captureFurnace(Level level, BlockPos pos, BlockState state, AbstractFurnaceBlockEntity furnace, CallbackInfo ci) {
        lunararc$tickLevel = level instanceof ServerLevel serverLevel && org.bukkit.Bukkit.isPrimaryThread() ? serverLevel : null;
        lunararc$tickPos = pos;
        lunararc$burnEvent = null;
    }

    @WrapOperation(method = "serverTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;getBurnDuration(Lnet/minecraft/world/item/ItemStack;)I"))
    private static int lunararc$furnaceBurn(AbstractFurnaceBlockEntity furnace, ItemStack fuel, Operation<Integer> original) {
        int duration = original.call(furnace, fuel);
        if (lunararc$tickLevel == null) return duration;
        FurnaceBurnEvent event = new FurnaceBurnEvent(CraftBlock.at(lunararc$tickLevel, lunararc$tickPos), CraftItemStack.asCraftMirror(fuel), duration);
        org.bukkit.Bukkit.getPluginManager().callEvent(event);
        lunararc$burnEvent = event;
        return event.isCancelled() ? 0 : event.getBurnTime();
    }

    @WrapOperation(method = "serverTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"))
    private static void lunararc$consumeFuel(ItemStack fuel, int amount, Operation<Void> original) {
        FurnaceBurnEvent event = lunararc$burnEvent;
        if (event != null && (!event.isBurning() || !event.willConsumeFuel())) return;
        original.call(fuel, amount);
    }

    @Inject(method = "burn", cancellable = true, at = @At(value = "INVOKE", ordinal = 1,
            target = "Lnet/minecraft/core/NonNullList;get(I)Ljava/lang/Object;"))
    private static void lunararc$furnaceSmelt(CallbackInfoReturnable<Boolean> cir,
            @Local(argsOnly = true) RecipeHolder<?> recipe, @Local(argsOnly = true) NonNullList<ItemStack> slots,
            @Local(ordinal = 1) LocalRef<ItemStack> result) {
        if (lunararc$tickLevel == null) return;
        org.bukkit.inventory.ItemStack bukkitResult = CraftItemStack.asBukkitCopy(result.get());
        org.bukkit.inventory.Recipe bukkitRecipe = CraftRecipeAdapter.toBukkit(recipe);
        FurnaceSmeltEvent event = new FurnaceSmeltEvent(CraftBlock.at(lunararc$tickLevel, lunararc$tickPos),
                CraftItemStack.asCraftMirror(slots.get(0)), bukkitResult,
                bukkitRecipe instanceof org.bukkit.inventory.CookingRecipe<?> cooking ? cooking : null);
        org.bukkit.Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            cir.setReturnValue(false);
            return;
        }
        if (!event.getResult().equals(bukkitResult)) {
            ItemStack changed = CraftItemStack.asNMSCopy(event.getResult());
            ItemStack output = slots.get(2);
            if (!output.isEmpty() && !ItemStack.isSameItemSameComponents(output, changed)) {
                cir.setReturnValue(false);
                return;
            }
            result.set(changed);
        }
    }

    @com.llamalad7.mixinextras.injector.ModifyExpressionValue(method = "serverTick", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;canBurn(Lnet/minecraft/core/RegistryAccess;Lnet/minecraft/world/item/crafting/RecipeHolder;Lnet/minecraft/core/NonNullList;I)Z"))
    private static boolean lunararc$startSmelt(boolean can, @Local(argsOnly = true) AbstractFurnaceBlockEntity furnace,
            @Local(argsOnly = true) Level level, @Local(argsOnly = true) BlockPos pos) {
        io.lunararcdevs.lunararc.common.bridge.world.CookSpeedBridge bridge = (io.lunararcdevs.lunararc.common.bridge.world.CookSpeedBridge) furnace;
        if (can && bridge.lunararc$isLit() && bridge.lunararc$cookingProgress() == 0) {
            bridge.lunararc$setCookingTotalTime(io.lunararcdevs.lunararc.common.event.LunarArcPaperEvents.furnaceStart(
                    level, pos, furnace.getItem(0), bridge.lunararc$recipeType(),
                    bridge.lunararc$cookingTotalTime() > 0 ? bridge.lunararc$cookingTotalTime() : shadowTotalCookTime(level, furnace)));
        }
        return can;
    }
}
