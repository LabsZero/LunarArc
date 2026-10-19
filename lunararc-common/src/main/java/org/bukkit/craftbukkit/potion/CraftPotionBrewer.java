package org.bukkit.craftbukkit.potion;

import com.google.common.base.Preconditions;
import io.papermc.paper.potion.PotionMix;
import java.util.Collection;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.potion.PotionBrewer;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.NotNull;

public final class CraftPotionBrewer implements PotionBrewer {
    private final MinecraftServer server;

    public CraftPotionBrewer(MinecraftServer server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    @Override
    @Deprecated(forRemoval = true)
    public @NotNull Collection<PotionEffect> getEffects(@NotNull PotionType type, boolean upgraded, boolean extended) {
        Objects.requireNonNull(type, "type");
        NamespacedKey key = type.getKey();
        Preconditions.checkArgument(!key.getKey().startsWith("strong_"),
                "Strong potion type cannot be used directly, got %s", key);
        Preconditions.checkArgument(!key.getKey().startsWith("long_"),
                "Extended potion type cannot be used directly, got %s", key);

        NamespacedKey effective = key;
        if (upgraded) {
            effective = new NamespacedKey(key.getNamespace(), "strong_" + key.getKey());
        } else if (extended) {
            effective = new NamespacedKey(key.getNamespace(), "long_" + key.getKey());
        }
        PotionType effectiveType = Registry.POTION.get(effective);
        Preconditions.checkNotNull(effectiveType, "Unknown potion type from data %s", effective);
        return effectiveType.getPotionEffects();
    }

    @Override
    public void resetPotionMixes() {
        // Brewing mixes are ordinary BrewingRecipe entries in the recipe manager in 26.x; drop the ones added by plugins
        // (anything outside the vanilla namespace) and leave vanilla/loader recipes in place.
        io.lunararcdevs.lunararc.common.bridge.recipe.RecipeManagerBridge bridge =
                (io.lunararcdevs.lunararc.common.bridge.recipe.RecipeManagerBridge) (Object) this.server.getRecipeManager();
        java.util.List<net.minecraft.world.item.crafting.RecipeHolder<?>> custom = new java.util.ArrayList<>();
        for (net.minecraft.world.item.crafting.RecipeHolder<?> holder : bridge.lunararc$recipes()) {
            if (holder.value().getType() == net.minecraft.world.item.crafting.RecipeType.BREWING
                    && !"minecraft".equals(holder.id().identifier().getNamespace())) {
                custom.add(holder);
            }
        }
        for (net.minecraft.world.item.crafting.RecipeHolder<?> holder : custom) {
            bridge.lunararc$removeRecipe(holder.id().identifier());
        }
    }
}
