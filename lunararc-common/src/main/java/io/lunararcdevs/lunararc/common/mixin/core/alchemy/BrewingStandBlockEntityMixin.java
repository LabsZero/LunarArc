package io.lunararcdevs.lunararc.common.mixin.core.alchemy;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.lunararcdevs.lunararc.common.bridge.alchemy.BrewingStandBridge;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.BrewingInput;
import net.minecraft.world.item.crafting.BrewingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Allows Paper custom potion inputs into the three brewing input slots. */
@Mixin(BrewingStandBlockEntity.class)
public abstract class BrewingStandBlockEntityMixin implements BrewingStandBridge {
    @Shadow int brewTime;
    public int recipeBrewTime = 400;
    @Shadow int fuel;

    @Unique private static final ThreadLocal<Map<ItemStack, ItemStack>> lunararc$brewResults = new ThreadLocal<>();

    @Override public int lunararc$getBrewTime() { return brewTime; }
    @Override public void lunararc$setBrewTime(int ticks) { brewTime = ticks; }
    @Override public int lunararc$getRecipeBrewTime() { return recipeBrewTime; }

    // 26.x computes the brew time as ceil(400.0F / fuel speed multiplier), so the base is a float constant
    @ModifyExpressionValue(method = "serverTick", at = @At(value = "CONSTANT", args = "floatValue=400.0"))
    private static float lunararc$recipeBrewTime(float original, ServerLevel level, BlockPos pos, BlockState state, BrewingStandBlockEntity stand) {
        return io.lunararcdevs.lunararc.common.event.LunarArcPaperEvents.brewingStart(
                level, pos, stand.getItem(3), ((BrewingStandBridge) stand).lunararc$getRecipeBrewTime());
    }

    @Override public int lunararc$getFuel() { return fuel; }
    @Override public void lunararc$setFuel(int fuel) { this.fuel = fuel; }

    @WrapMethod(method = "serverTick")
    private static void lunararc$fuelEvent(ServerLevel level, BlockPos pos, BlockState state,
            BrewingStandBlockEntity stand, Operation<Void> original) {
        BrewingStandBridge bridge = (BrewingStandBridge) stand;
        ItemStack fuelStack = stand.getItem(4);
        if (bridge.lunararc$getFuel() <= 0 && fuelStack.is(Items.BLAZE_POWDER)) {
            ServerLevel serverLevel = level;
            org.bukkit.event.inventory.BrewingStandFuelEvent event = new org.bukkit.event.inventory.BrewingStandFuelEvent(
                    org.bukkit.craftbukkit.block.CraftBlock.create(serverLevel, pos),
                    org.bukkit.craftbukkit.inventory.CraftItemStack.asBukkitCopy(fuelStack), 20);
            org.bukkit.Bukkit.getPluginManager().callEvent(event);

            int power = Math.max(0, event.getFuelPower());
            if (event.isCancelled() || power == 0) {
                // Hide only the fuel stack from the vanilla refuel prelude. All other tick logic still runs.
                ItemStack saved = fuelStack.copy();
                stand.setItem(4, ItemStack.EMPTY);
                try {
                    original.call(level, pos, state, stand);
                } finally {
                    stand.setItem(4, saved);
                }
                return;
            }

            bridge.lunararc$setFuel(power);
            if (event.isConsuming()) fuelStack.shrink(1);
            stand.setChanged();
        }
        original.call(level, pos, state, stand);
    }

    @WrapMethod(method = "doBrew")
    private static void lunararc$brewEvent(ServerLevel level, BlockPos pos, BrewingStandBlockEntity stand, Operation<Void> original) {
        NonNullList<ItemStack> items = stand.getItems();
        ItemStack ingredient = items.get(3);
        Map<ItemStack, ItemStack> defaults = new IdentityHashMap<>();
        List<org.bukkit.inventory.ItemStack> results = new ArrayList<>(3);
        for (int i = 0; i < 3; i++) {
            ItemStack bottle = items.get(i);
            BrewingInput input = new BrewingInput(bottle, ingredient);
            ItemStack result = level.getServer().getRecipeManager()
                    .getRecipeFor(RecipeType.BREWING, input, level)
                    .map(holder -> holder.value().assemble(input))
                    .orElse(bottle);
            defaults.put(bottle, result);
            results.add(org.bukkit.craftbukkit.inventory.CraftItemStack.asBukkitCopy(result));
        }

        org.bukkit.event.inventory.BrewEvent event = new org.bukkit.event.inventory.BrewEvent(
                org.bukkit.craftbukkit.block.CraftBlock.create(level, pos),
                new org.bukkit.craftbukkit.inventory.CraftBrewerInventory(stand),
                results, ((BrewingStandBridge) stand).lunararc$getFuel());
        org.bukkit.Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        Map<ItemStack, ItemStack> chosen = new IdentityHashMap<>();
        List<org.bukkit.inventory.ItemStack> eventResults = event.getResults();
        for (int i = 0; i < 3; i++) {
            org.bukkit.inventory.ItemStack result = i < eventResults.size() ? eventResults.get(i) : null;
            ItemStack nms = org.bukkit.craftbukkit.inventory.CraftItemStack.asNMSCopy(result);
            chosen.put(items.get(i), nms.isEmpty() ? ItemStack.EMPTY : nms);
        }

        lunararc$brewResults.set(chosen);
        try {
            original.call(level, pos, stand);
        } finally {
            lunararc$brewResults.remove();
        }
    }

    @WrapOperation(method = "doBrew", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/crafting/BrewingRecipe;assemble(Lnet/minecraft/world/item/crafting/BrewingInput;)Lnet/minecraft/world/item/ItemStack;"), require = 0)
    private static ItemStack lunararc$applyBukkitBrewResult(BrewingRecipe recipe, BrewingInput input, Operation<ItemStack> original) {
        Map<ItemStack, ItemStack> chosen = lunararc$brewResults.get();
        if (chosen != null) {
            ItemStack override = chosen.get(input.input());
            if (override != null) return override.copy();
        }
        return original.call(recipe, input);
    }

    @Inject(method = "canPlaceItem", at = @At("HEAD"), cancellable = true)
    private void lunararc$allowPaperPotionInput(int slot, ItemStack stack,
            CallbackInfoReturnable<Boolean> cir) {
        if (slot < 0 || slot > 2) return;
        BrewingStandBlockEntity self = (BrewingStandBlockEntity) (Object) this;
        if (self.getLevel() == null || !self.getItem(slot).isEmpty()) return;
        if (!(self.getLevel() instanceof ServerLevel serverLevel)) return;
        boolean accepted = serverLevel.getServer().getRecipeManager().recipes.byType(RecipeType.BREWING).stream()
                .anyMatch(holder -> holder.value().getInput().test(stack));
        if (accepted) {
            cir.setReturnValue(true);
        }
    }
}
