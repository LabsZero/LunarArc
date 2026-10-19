package org.bukkit.craftbukkit.inventory;

import com.google.common.base.Preconditions;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.data.util.Conversions;
import io.papermc.paper.registry.set.PaperRegistrySets;
import io.papermc.paper.registry.set.RegistryKeySet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Ingredient;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.util.CraftNamespacedKey;
import org.bukkit.inventory.ItemType;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.recipe.CookingBookCategory;
import org.bukkit.inventory.recipe.CraftingBookCategory;

public interface CraftRecipe extends Recipe {

    net.minecraft.world.item.crafting.RecipeHolder<?> toMinecraftRecipe();

    default void addToRecipeManager() {
        io.lunararcdevs.lunararc.common.mod.server.LunarArcServer.minecraftServer().getRecipeManager();
        ((io.lunararcdevs.lunararc.common.bridge.recipe.RecipeManagerBridge) (Object) io.lunararcdevs.lunararc.common.mod.server.LunarArcServer.minecraftServer().getRecipeManager())
                .lunararc$addRecipe(this.toMinecraftRecipe());
    }

    static Optional<Ingredient> toPossibleIngredient(RecipeChoice bukkit, boolean requireNotEmpty) {
        return (bukkit == null || bukkit == RecipeChoice.empty()) ? Optional.empty() : Optional.of(toIngredient(bukkit, requireNotEmpty));
    }

    static CraftRecipe fromBukkitRecipe(Recipe recipe) {
        return switch (recipe) {
            case CraftRecipe craftRecipe -> craftRecipe;
            case org.bukkit.inventory.ShapedRecipe shapedRecipe -> CraftShapedRecipe.fromBukkitRecipe(shapedRecipe);
            case org.bukkit.inventory.ShapelessRecipe shapelessRecipe -> CraftShapelessRecipe.fromBukkitRecipe(shapelessRecipe);
            case org.bukkit.inventory.FurnaceRecipe furnaceRecipe -> CraftFurnaceRecipe.fromBukkitRecipe(furnaceRecipe);
            case org.bukkit.inventory.BlastingRecipe blastingRecipe -> CraftBlastingRecipe.fromBukkitRecipe(blastingRecipe);
            case org.bukkit.inventory.CampfireRecipe campfireRecipe -> CraftCampfireRecipe.fromBukkitRecipe(campfireRecipe);
            case org.bukkit.inventory.SmokingRecipe smokingRecipe -> CraftSmokingRecipe.fromBukkitRecipe(smokingRecipe);
            case org.bukkit.inventory.StonecuttingRecipe stonecuttingRecipe -> CraftStonecuttingRecipe.fromBukkitRecipe(stonecuttingRecipe);
            case org.bukkit.inventory.SmithingTransformRecipe smithingTransformRecipe -> CraftSmithingTransformRecipe.fromBukkitRecipe(smithingTransformRecipe);
            case org.bukkit.inventory.SmithingTrimRecipe smithingTrimRecipe -> CraftSmithingTrimRecipe.fromBukkitRecipe(smithingTrimRecipe);
            case org.bukkit.inventory.TransmuteRecipe transmuteRecipe -> CraftTransmuteRecipe.fromBukkitRecipe(transmuteRecipe);
            case org.bukkit.inventory.BrewingRecipe brewingRecipe -> CraftBrewingRecipe.fromBukkitRecipe(brewingRecipe);
            case org.bukkit.inventory.ComplexRecipe ignored -> throw new UnsupportedOperationException("Cannot convert custom complex recipe");
            default -> null;
        };
    }

    default Optional<Ingredient> toNMSOptional(RecipeChoice bukkit, boolean requireNotEmpty) {
        return (bukkit == null || bukkit == RecipeChoice.empty()) ? Optional.empty() : Optional.of(this.toNMS(bukkit, requireNotEmpty)); // Paper - support "empty" choices
    }

    default Ingredient toNMS(RecipeChoice bukkit, boolean requireNotEmpty) {
        // Paper start
        return toIngredient(bukkit, requireNotEmpty);
    }
    static Ingredient toIngredient(RecipeChoice bukkit, boolean requireNotEmpty) {
        // Paper end
        Ingredient stack;

        if (bukkit == null) {
            stack = Ingredient.of();
        } else if (bukkit instanceof final RecipeChoice.ItemTypeChoice itemTypeChoice) {
            stack = Ingredient.of(PaperRegistrySets.convertToNms(Registries.ITEM, Conversions.global().lookup(), itemTypeChoice.itemTypes()));
        } else if (bukkit instanceof RecipeChoice.MaterialChoice) {
            stack = Ingredient.of(((RecipeChoice.MaterialChoice) bukkit).getChoices().stream().map((mat) -> CraftItemType.bukkitToMinecraft(mat)));
        } else if (bukkit instanceof RecipeChoice.ExactChoice) {
            stack = io.lunararcdevs.lunararc.common.util.LunarArcIngredients.ofStacks(((RecipeChoice.ExactChoice) bukkit).getChoices().stream().map((mat) -> CraftItemStack.asNMSCopy(mat)).toList());
            // Paper start - support "empty" choices - legacy method that spigot might incorrectly call
            // Their impl of Ingredient.of() will error, ingredients need at least one entry.
            // Callers running into this exception may have passed an incorrect empty() recipe choice to a non-empty slot or
            // spigot calls this method in a wrong place.
        } else if (bukkit == RecipeChoice.empty()) {
            throw new IllegalArgumentException("This ingredient cannot be empty");
            // Paper end - support "empty" choices
        } else {
            throw new IllegalArgumentException("Unknown recipe stack instance " + bukkit);
        }

        if (requireNotEmpty) {
            Preconditions.checkArgument(!stack.isEmpty(), "Recipe requires at least one non-air choice");
        }

        return stack;
    }

    public static RecipeChoice toBukkit(Optional<Ingredient> list) {
        return list.map(CraftRecipe::toBukkit).orElse(RecipeChoice.empty()); // Paper - fix issue with recipe API
    }

    public static RecipeChoice toBukkit(Ingredient list) {
        if (list.isEmpty()) {
            return RecipeChoice.empty(); // Paper - null breaks API contracts
        }

        if ((((io.lunararcdevs.lunararc.common.bridge.recipe.IngredientBridge) (Object) list).lunararc$exactStacks() != null)) {
            List<org.bukkit.inventory.ItemStack> choices = new ArrayList<>(((io.lunararcdevs.lunararc.common.bridge.recipe.IngredientBridge) (Object) list).lunararc$exactStacks().size());
            for (net.minecraft.world.item.ItemStack i : ((io.lunararcdevs.lunararc.common.bridge.recipe.IngredientBridge) (Object) list).lunararc$exactStacks()) {
                choices.add(CraftItemStack.asBukkitCopy(i));
            }

            return new RecipeChoice.ExactChoice(choices);
        } else {
            final RegistryKeySet<ItemType> itemTypes = PaperRegistrySets.convertToApi(RegistryKey.ITEM, list.values);
            return RecipeChoice.itemType(itemTypes);
        }
    }

    public static net.minecraft.world.item.crafting.CraftingBookCategory getCategory(CraftingBookCategory bukkit) {
        return net.minecraft.world.item.crafting.CraftingBookCategory.valueOf(bukkit.name());
    }

    public static CraftingBookCategory getCategory(net.minecraft.world.item.crafting.CraftingBookCategory nms) {
        return CraftingBookCategory.valueOf(nms.name());
    }

    public static net.minecraft.world.item.crafting.CookingBookCategory getCategory(CookingBookCategory bukkit) {
        return net.minecraft.world.item.crafting.CookingBookCategory.valueOf(bukkit.name());
    }

    public static CookingBookCategory getCategory(net.minecraft.world.item.crafting.CookingBookCategory nms) {
        return CookingBookCategory.valueOf(nms.name());
    }

    public static ResourceKey<net.minecraft.world.item.crafting.Recipe<?>> toMinecraft(NamespacedKey key) {
        return ResourceKey.create(Registries.RECIPE, CraftNamespacedKey.toMinecraft(key));
    }
}
