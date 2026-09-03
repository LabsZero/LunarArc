package org.bukkit.craftbukkit.inventory;

import net.minecraft.world.item.crafting.Ingredient;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Server-owned RecipeChoice that delegates matching to the real loader-owned
 * 1.21.1 NMS Ingredient. This preserves modloader/custom ingredient semantics
 * without introducing a platform dispatcher or pretending every ingredient is
 * a vanilla MaterialChoice.
 */
public final class CraftNmsRecipeChoice implements RecipeChoice {
    private final Ingredient handle;

    public CraftNmsRecipeChoice(Ingredient handle) {
        this.handle = Objects.requireNonNull(handle, "handle");
    }

    public Ingredient getHandle() {
        return this.handle;
    }

    @Override
    @Deprecated
    public @NotNull ItemStack getItemStack() {
        net.minecraft.world.item.ItemStack[] choices = this.handle.getItems();
        if (choices.length == 0) return new ItemStack(Material.AIR);
        return CraftItemStack.asBukkitCopy(choices[0]);
    }

    @Override
    public @NotNull CraftNmsRecipeChoice clone() {
        // Ingredient instances are registry/recipe state owned by Minecraft.
        // The wrapper is the mutable API object; sharing the immutable recipe
        // predicate handle is intentional and preserves loader semantics.
        return new CraftNmsRecipeChoice(this.handle);
    }

    @Override
    public boolean test(@NotNull ItemStack itemStack) {
        Objects.requireNonNull(itemStack, "itemStack");
        return this.handle.test(CraftItemStack.asNMSCopy(itemStack));
    }

    @Override
    public String toString() {
        return "CraftNmsRecipeChoice[" + this.handle + "]";
    }
}
