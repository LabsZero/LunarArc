package org.bukkit.craftbukkit.inventory;

import com.google.common.collect.Maps;
import io.lunararcdevs.lunararc.common.bridge.access.ShapedRecipeAccessBridge;
import io.lunararcdevs.lunararc.common.bridge.access.ShapelessRecipeAccessBridge;
import io.lunararcdevs.lunararc.common.bridge.access.SingleItemRecipeAccessBridge;
import io.lunararcdevs.lunararc.common.bridge.access.SmithingTransformRecipeAccessBridge;
import io.lunararcdevs.lunararc.common.bridge.access.SmithingTrimRecipeAccessBridge;
import io.lunararcdevs.lunararc.common.bridge.recipe.CopyDataComponentsBridge;
import io.lunararcdevs.lunararc.common.bridge.recipe.IngredientBridge;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.TransmuteResult;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.BlastingRecipe;
import org.bukkit.inventory.CampfireRecipe;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.SmithingTransformRecipe;
import org.bukkit.inventory.SmithingTrimRecipe;
import org.bukkit.inventory.SmokingRecipe;
import org.bukkit.inventory.StonecuttingRecipe;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Converts between Bukkit recipes and the 1.21.11 vanilla recipe objects. Vanilla recipe state is read through the
 * accessor mixins in {@code mixin.core.recipe}; "exact" choices are carried on the Ingredient itself.
 */
public final class CraftRecipeAdapter {
    private static final char[] SHAPE_SYMBOLS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

    private CraftRecipeAdapter() {}

    public static Identifier toMinecraft(NamespacedKey key) {
        Objects.requireNonNull(key, "key");
        return Identifier.fromNamespaceAndPath(key.getNamespace(), key.getKey());
    }

    public static ResourceKey<net.minecraft.world.item.crafting.Recipe<?>> toRecipeKey(NamespacedKey key) {
        return ResourceKey.create(Registries.RECIPE, toMinecraft(key));
    }

    public static NamespacedKey toBukkit(Identifier key) {
        Objects.requireNonNull(key, "key");
        return new NamespacedKey(key.getNamespace(), key.getPath());
    }

    public static NamespacedKey toBukkit(ResourceKey<net.minecraft.world.item.crafting.Recipe<?>> key) {
        Objects.requireNonNull(key, "key");
        return toBukkit(key.identifier());
    }

    private static net.minecraft.world.item.Item item(Material material) {
        return CraftItemType.bukkitToMinecraft(material);
    }

    /** Convert a choice into an Ingredient; empty choices become an empty ingredient. */
    public static Ingredient toIngredient(RecipeChoice choice, boolean requireNotEmpty) {
        if (choice instanceof CraftNmsRecipeChoice nmsChoice) {
            return nmsChoice.getHandle();
        }

        final Ingredient ingredient;
        if (choice == null || choice == RecipeChoice.empty()) {
            ingredient = Ingredient.of();
        } else if (choice instanceof RecipeChoice.MaterialChoice materialChoice) {
            ingredient = Ingredient.of(materialChoice.getChoices().stream()
                    .filter(material -> material != Material.AIR)
                    .map(CraftRecipeAdapter::item));
        } else if (choice instanceof RecipeChoice.ExactChoice exactChoice) {
            List<net.minecraft.world.item.ItemStack> stacks = exactChoice.getChoices().stream()
                    .map(CraftItemStack::asNMSCopy)
                    .filter(stack -> !stack.isEmpty())
                    .toList();
            ingredient = Ingredient.of(stacks.stream().map(net.minecraft.world.item.ItemStack::getItem));
            ((IngredientBridge) (Object) ingredient).lunararc$setExactStacks(stacks);
        } else if (choice instanceof RecipeChoice.ItemTypeChoice itemTypeChoice) {
            ingredient = Ingredient.of(itemTypeChoice.itemTypes()
                    .resolve(io.papermc.paper.registry.RegistryAccess.registryAccess().getRegistry(io.papermc.paper.registry.RegistryKey.ITEM))
                    .stream().map(CraftItemType::bukkitToMinecraftNew));
        } else {
            throw new IllegalArgumentException("Unsupported RecipeChoice implementation: " + choice.getClass().getName());
        }

        if (requireNotEmpty && ingredient.isEmpty()) {
            throw new IllegalArgumentException("Recipe requires at least one non-air choice");
        }
        return ingredient;
    }

    public static Optional<Ingredient> toOptionalIngredient(RecipeChoice choice) {
        return choice == null || choice == RecipeChoice.empty() ? Optional.empty() : Optional.of(toIngredient(choice, false));
    }

    /**
     * Preserve the real NMS predicate for loader/custom ingredients. Exact choices retain Paper's ExactChoice API
     * shape; all other NMS ingredients use a server-owned RecipeChoice that delegates test() to Ingredient.
     */
    public static RecipeChoice toBukkit(Ingredient ingredient) {
        Objects.requireNonNull(ingredient, "ingredient");
        if (ingredient.isEmpty()) return RecipeChoice.empty();

        List<net.minecraft.world.item.ItemStack> exact = ((IngredientBridge) (Object) ingredient).lunararc$exactStacks();
        if (exact != null) {
            List<ItemStack> choices = new ArrayList<>(exact.size());
            for (net.minecraft.world.item.ItemStack stack : exact) {
                if (!stack.isEmpty()) choices.add(CraftItemStack.asBukkitCopy(stack));
            }
            if (!choices.isEmpty()) return new RecipeChoice.ExactChoice(choices);
        }

        return new CraftNmsRecipeChoice(ingredient);
    }

    public static RecipeChoice toBukkit(Optional<Ingredient> ingredient) {
        return ingredient.map(CraftRecipeAdapter::toBukkit).orElse(RecipeChoice.empty());
    }

    private static CraftingBookCategory craftingCategory(org.bukkit.inventory.recipe.CraftingBookCategory category) {
        return CraftingBookCategory.valueOf(category.name());
    }

    private static org.bukkit.inventory.recipe.CraftingBookCategory bukkitCraftingCategory(CraftingBookCategory category) {
        return org.bukkit.inventory.recipe.CraftingBookCategory.valueOf(category.name());
    }

    private static CookingBookCategory cookingCategory(org.bukkit.inventory.recipe.CookingBookCategory category) {
        return CookingBookCategory.valueOf(category.name());
    }

    private static org.bukkit.inventory.recipe.CookingBookCategory bukkitCookingCategory(CookingBookCategory category) {
        return org.bukkit.inventory.recipe.CookingBookCategory.valueOf(category.name());
    }

    public static RecipeHolder<?> toMinecraft(Recipe recipe) {
        Objects.requireNonNull(recipe, "recipe");
        if (!(recipe instanceof Keyed)) {
            throw new IllegalArgumentException("Bukkit recipe must implement Keyed: " + recipe.getClass().getName());
        }
        if (recipe instanceof CraftComplexRecipe complex) {
            return complex.getHandle();
        }
        CraftRecipe converted = CraftRecipe.fromBukkitRecipe(recipe);
        if (converted == null) {
            throw new UnsupportedOperationException("Unsupported Bukkit recipe type: " + recipe.getClass().getName());
        }
        return converted.toMinecraftRecipe();
    }

    /** Convert a real 26.x NMS recipe into the corresponding Bukkit type. */
    public static Recipe toBukkit(RecipeHolder<?> holder) {
        Objects.requireNonNull(holder, "holder");
        NamespacedKey key = toBukkit(holder.id());
        net.minecraft.world.item.crafting.Recipe<?> nms = holder.value();

        if (nms instanceof net.minecraft.world.item.crafting.ShapedRecipe shaped) {
            ShapedRecipeAccessBridge access = (ShapedRecipeAccessBridge) (Object) shaped;
            ShapedRecipePattern pattern = access.lunararc$pattern();
            int width = pattern.width();
            int height = pattern.height();
            if (width < 1 || height < 1 || width > 3 || height > 3 || width * height > SHAPE_SYMBOLS.length) {
                return new CraftComplexRecipe(holder);
            }

            ShapedRecipe recipe = new ShapedRecipe(key, CraftItemStack.asBukkitCopy(access.lunararc$result().create()));
            recipe.setGroup(shaped.group());
            recipe.setCategory(bukkitCraftingCategory(shaped.category()));

            List<Optional<Ingredient>> ingredients = pattern.ingredients();
            String[] shape = new String[height];
            int symbolIndex = 0;
            Map<Character, Ingredient> mapped = new LinkedHashMap<>();
            for (int y = 0; y < height; y++) {
                StringBuilder row = new StringBuilder(width);
                for (int x = 0; x < width; x++) {
                    int index = y * width + x;
                    Ingredient ingredient = index < ingredients.size() ? ingredients.get(index).orElse(null) : null;
                    if (ingredient == null || ingredient.isEmpty()) {
                        row.append(' ');
                    } else {
                        char symbol = SHAPE_SYMBOLS[symbolIndex++];
                        row.append(symbol);
                        mapped.put(symbol, ingredient);
                    }
                }
                shape[y] = row.toString();
            }
            recipe.shape(shape);
            mapped.forEach((symbol, ingredient) -> recipe.setIngredient(symbol, toBukkit(ingredient)));
            return recipe;
        }

        if (nms instanceof net.minecraft.world.item.crafting.ShapelessRecipe shapeless) {
            ShapelessRecipeAccessBridge access = (ShapelessRecipeAccessBridge) (Object) shapeless;
            ShapelessRecipe recipe = new ShapelessRecipe(key, CraftItemStack.asBukkitCopy(access.lunararc$result().create()));
            recipe.setGroup(shapeless.group());
            recipe.setCategory(bukkitCraftingCategory(shapeless.category()));
            for (Ingredient ingredient : access.lunararc$ingredients()) {
                if (ingredient != null && !ingredient.isEmpty()) recipe.addIngredient(toBukkit(ingredient));
            }
            return recipe;
        }

        if (nms instanceof AbstractCookingRecipe cooking) {
            SingleItemRecipeAccessBridge access = (SingleItemRecipeAccessBridge) (Object) cooking;
            ItemStack result = CraftItemStack.asBukkitCopy(access.lunararc$result().create());
            RecipeChoice input = toBukkit(cooking.input());
            String group = cooking.group();
            float experience = cooking.experience();
            int time = cooking.cookingTime();
            org.bukkit.inventory.recipe.CookingBookCategory category = bukkitCookingCategory(cooking.category());

            if (nms instanceof net.minecraft.world.item.crafting.BlastingRecipe) {
                BlastingRecipe recipe = new BlastingRecipe(key, result, input, experience, time);
                recipe.setGroup(group); recipe.setCategory(category); return recipe;
            }
            if (nms instanceof net.minecraft.world.item.crafting.CampfireCookingRecipe) {
                CampfireRecipe recipe = new CampfireRecipe(key, result, input, experience, time);
                recipe.setGroup(group); recipe.setCategory(category); return recipe;
            }
            if (nms instanceof net.minecraft.world.item.crafting.SmokingRecipe) {
                SmokingRecipe recipe = new SmokingRecipe(key, result, input, experience, time);
                recipe.setGroup(group); recipe.setCategory(category); return recipe;
            }
            if (nms instanceof net.minecraft.world.item.crafting.SmeltingRecipe) {
                FurnaceRecipe recipe = new FurnaceRecipe(key, result, input, experience, time);
                recipe.setGroup(group); recipe.setCategory(category); return recipe;
            }
            return new CraftComplexRecipe(holder);
        }

        if (nms instanceof net.minecraft.world.item.crafting.StonecutterRecipe stonecutter) {
            SingleItemRecipeAccessBridge access = (SingleItemRecipeAccessBridge) (Object) stonecutter;
            StonecuttingRecipe recipe = new StonecuttingRecipe(key,
                    CraftItemStack.asBukkitCopy(access.lunararc$result().create()), toBukkit(stonecutter.input()));
            recipe.setGroup(stonecutter.group());
            return recipe;
        }

        if (nms instanceof net.minecraft.world.item.crafting.SmithingTransformRecipe smithing) {
            SmithingTransformRecipeAccessBridge access = (SmithingTransformRecipeAccessBridge) (Object) smithing;
            boolean copy = ((CopyDataComponentsBridge) (Object) smithing).lunararc$copyDataComponents();
            return new SmithingTransformRecipe(key, CraftItemStack.asBukkitCopy(access.lunararc$result().create()),
                    toBukkit(smithing.templateIngredient()), toBukkit(smithing.baseIngredient()),
                    toBukkit(smithing.additionIngredient()), copy);
        }

        if (nms instanceof net.minecraft.world.item.crafting.SmithingTrimRecipe smithing) {
            SmithingTrimRecipeAccessBridge access = (SmithingTrimRecipeAccessBridge) (Object) smithing;
            boolean copy = ((CopyDataComponentsBridge) (Object) smithing).lunararc$copyDataComponents();
            return new SmithingTrimRecipe(key, toBukkit(smithing.templateIngredient()),
                    toBukkit(smithing.baseIngredient()), toBukkit(smithing.additionIngredient()),
                    org.bukkit.craftbukkit.inventory.trim.CraftTrimPattern.minecraftHolderToBukkit(access.lunararc$pattern()), copy);
        }

        // Vanilla special recipes and loader-defined imperative recipes have no
        // stable shaped/cooking representation. ComplexRecipe is Bukkit's
        // concrete contract for server-defined recipe behavior.
        return new CraftComplexRecipe(holder);
    }
}
