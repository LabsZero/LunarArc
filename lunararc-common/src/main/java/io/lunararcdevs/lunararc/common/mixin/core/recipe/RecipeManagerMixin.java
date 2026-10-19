package io.lunararcdevs.lunararc.common.mixin.core.recipe;

import io.lunararcdevs.lunararc.common.bridge.recipe.RecipeManagerBridge;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * CraftBukkit recipe mutation hooks applied to the real Minecraft RecipeManager. 1.21.2+ keeps all recipes in an
 * immutable {@link RecipeMap}; every mutation rebuilds the map and re-runs vanilla's recipe finalisation so the
 * synchronised recipe-book data stays consistent.
 */
@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin implements RecipeManagerBridge {
    @Shadow public RecipeMap recipes;
    @Shadow public abstract void finalizeRecipeLoading(FeatureFlagSet enabledFeatures);

    @Unique private FeatureFlagSet lunararc$featureFlags;

    @Unique
    private static RecipeMap lunararc$build(Collection<RecipeHolder<?>> all) {
        com.google.common.collect.Multimap<net.minecraft.world.item.crafting.RecipeType<?>, RecipeHolder<?>> byType = com.google.common.collect.ArrayListMultimap.create();
        java.util.Map<ResourceKey<net.minecraft.world.item.crafting.Recipe<?>>, RecipeHolder<?>> byKey = new java.util.LinkedHashMap<>();
        for (RecipeHolder<?> holder : all) {
            byType.put(holder.value().getType(), holder);
            byKey.put(holder.id(), holder);
        }
        return new RecipeMap(byType, byKey);
    }

    @Inject(method = "finalizeRecipeLoading(Lnet/minecraft/world/flag/FeatureFlagSet;)V", at = @At("HEAD"))
    private void lunararc$captureFeatureFlags(FeatureFlagSet enabledFeatures, CallbackInfo ci) {
        this.lunararc$featureFlags = enabledFeatures;
    }

    @Unique
    private void lunararc$refresh() {
        if (this.lunararc$featureFlags != null) {
            this.finalizeRecipeLoading(this.lunararc$featureFlags);
            net.minecraft.server.MinecraftServer server = io.lunararcdevs.lunararc.common.mod.server.LunarArcServer.minecraftServer();
            if (server != null && server.getPlayerList() != null) server.getPlayerList().reloadResources();
        }
    }

    // --- Paper-compatible surface (patched vanilla): Paper classes call these directly ---
    public void addRecipe(RecipeHolder<?> holder) {
        this.lunararc$addRecipe(holder);
    }

    public boolean removeRecipe(ResourceKey<net.minecraft.world.item.crafting.Recipe<?>> key) {
        return this.lunararc$removeRecipe(key.identifier());
    }

    public void clearRecipes() {
        this.lunararc$clearRecipes();
    }

    public void finalizeRecipeLoading() {
        this.lunararc$refresh();
    }

    @Override
    public synchronized Collection<RecipeHolder<?>> lunararc$recipes() {
        return new ArrayList<>(this.recipes.values());
    }

    @Override
    public synchronized RecipeHolder<?> lunararc$recipe(Identifier id) {
        return this.recipes.byKey(ResourceKey.create(Registries.RECIPE, id));
    }

    @Override
    public synchronized boolean lunararc$addRecipe(RecipeHolder<?> recipe) {
        if (this.recipes.byKey.containsKey(recipe.id())) return false;
        List<RecipeHolder<?>> all = new ArrayList<>(this.recipes.values());
        all.add(recipe);
        this.recipes = lunararc$build(all);
        this.lunararc$refresh();
        return true;
    }

    @Override
    public synchronized boolean lunararc$removeRecipe(Identifier id) {
        ResourceKey<net.minecraft.world.item.crafting.Recipe<?>> key = ResourceKey.create(Registries.RECIPE, id);
        if (!this.recipes.byKey.containsKey(key)) return false;
        List<RecipeHolder<?>> all = new ArrayList<>(this.recipes.values());
        all.removeIf(holder -> holder.id().equals(key));
        this.recipes = lunararc$build(all);
        this.lunararc$refresh();
        return true;
    }

    @Override
    public synchronized void lunararc$clearRecipes() {
        this.recipes = lunararc$build(List.of());
        this.lunararc$refresh();
    }
}
