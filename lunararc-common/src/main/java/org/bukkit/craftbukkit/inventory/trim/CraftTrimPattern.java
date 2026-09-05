package org.bukkit.craftbukkit.inventory.trim;

import net.minecraft.network.chat.contents.TranslatableContents;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/** Concrete trim pattern backed by the active dynamic Minecraft registry. */
public final class CraftTrimPattern implements TrimPattern, org.bukkit.craftbukkit.util.Handleable<net.minecraft.world.item.armortrim.TrimPattern> {
    private final NamespacedKey key;
    private final net.minecraft.world.item.armortrim.TrimPattern handle;

    public CraftTrimPattern(@NotNull NamespacedKey key,
                            @NotNull net.minecraft.world.item.armortrim.TrimPattern handle) {
        this.key = Objects.requireNonNull(key, "key");
        this.handle = Objects.requireNonNull(handle, "handle");
    }

    public @NotNull net.minecraft.world.item.armortrim.TrimPattern getHandle() { return this.handle; }
    @Override public @NotNull NamespacedKey getKey() { return this.key; }
    @Override public @NotNull String getTranslationKey() {
        if (this.handle.description().getContents() instanceof TranslatableContents translatable) return translatable.getKey();
        return net.minecraft.Util.makeDescriptionId(
                "trim_pattern",
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(key.getNamespace(), key.getKey()));
    }
    @Override public net.kyori.adventure.text.Component description() {
        return io.papermc.paper.adventure.PaperAdventure.asAdventure(this.handle.description());
    }
    @Override public String toString() { return "CraftTrimPattern[" + this.key + "]"; }

    // CraftBukkit's conversion pair for this registry type. Plugins call these directly to cross
    // between the Bukkit handle and the NMS object, so they carry CraftBukkit's names verbatim.
    public static TrimPattern minecraftToBukkit(net.minecraft.world.item.armortrim.TrimPattern minecraft) {
        return org.bukkit.craftbukkit.CraftRegistry.minecraftToBukkit(minecraft, net.minecraft.core.registries.Registries.TRIM_PATTERN, org.bukkit.Registry.TRIM_PATTERN);
    }

    public static TrimPattern minecraftHolderToBukkit(net.minecraft.core.Holder<net.minecraft.world.item.armortrim.TrimPattern> minecraft) {
        return CraftTrimPattern.minecraftToBukkit(minecraft.value());
    }

    public static net.minecraft.world.item.armortrim.TrimPattern bukkitToMinecraft(TrimPattern bukkit) {
        return org.bukkit.craftbukkit.CraftRegistry.bukkitToMinecraft(bukkit);
    }

    public static net.minecraft.core.Holder<net.minecraft.world.item.armortrim.TrimPattern> bukkitToMinecraftHolder(TrimPattern bukkit) {
        com.google.common.base.Preconditions.checkArgument(bukkit != null);

        net.minecraft.core.Registry<net.minecraft.world.item.armortrim.TrimPattern> registry = org.bukkit.craftbukkit.CraftRegistry.getMinecraftRegistry(net.minecraft.core.registries.Registries.TRIM_PATTERN);

        if (registry.wrapAsHolder(CraftTrimPattern.bukkitToMinecraft(bukkit)) instanceof net.minecraft.core.Holder.Reference<net.minecraft.world.item.armortrim.TrimPattern> holder) {
            return holder;
        }

        throw new IllegalArgumentException("No Reference holder found for " + bukkit
                + ", this can happen if a plugin creates its own trim pattern without properly registering it.");
    }
}
