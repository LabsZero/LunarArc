package org.bukkit.craftbukkit;

import com.google.common.base.Preconditions;
import io.ampznetwork.lunararc.common.LunarArcServerAccess;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Biome;
import org.bukkit.craftbukkit.legacy.FieldRename;
import org.bukkit.craftbukkit.util.ApiVersion;
import org.bukkit.craftbukkit.util.CraftNamespacedKey;
import org.bukkit.craftbukkit.util.Handleable;
import org.bukkit.entity.EntityType;

import java.util.Optional;

/**
 * The conversion half of CraftBukkit's {@code CraftRegistry}.
 *
 * <p>Every {@code Craft*} type that wraps a registry entry - enchantments, potion types, banner
 * patterns, trim materials, damage types and the rest - delegates its {@code minecraftToBukkit} /
 * {@code bukkitToMinecraft} pair here rather than repeating the lookup. Plugins call those static
 * pairs directly to cross between a Bukkit handle and the NMS object, which is why they have to
 * exist under CraftBukkit's own names and signatures.
 *
 * <p>What is deliberately absent is the other half: real Paper's CraftRegistry is also
 * {@code CraftRegistry<B, M> implements Registry<B>}, the backing implementation of the Bukkit
 * registries themselves, together with the dynamic Registry Modification API (patches 0471, 0913,
 * 0920 and 1014 in PaperMC/Paper-archive ver/1.21.1). LunarArc's Bukkit registries are not built on
 * this class, so adding an instance side here would be a shim with nothing behind it. The methods
 * below are all real lookups against the live registry - nothing is stubbed - and
 * {@link #get(Registry, NamespacedKey, ApiVersion)} simply loses the branch that would have
 * consulted a CraftRegistry instance's own serialization updater.
 */
public final class CraftRegistry {
    private CraftRegistry() {}

    public static RegistryAccess getMinecraftRegistry() {
        return LunarArcServerAccess.getMinecraftServer().registryAccess();
    }

    public static <E> net.minecraft.core.Registry<E> getMinecraftRegistry(
            ResourceKey<net.minecraft.core.Registry<E>> key) {
        return getMinecraftRegistry().registryOrThrow(key);
    }

    /**
     * Usage note, carried over from CraftBukkit: only use this to delegate the conversion methods
     * of the individual Craft classes. Elsewhere, call the per-type method instead.
     */
    public static <B extends Keyed, M> B minecraftToBukkit(M minecraft,
            ResourceKey<net.minecraft.core.Registry<M>> registryKey, Registry<B> bukkitRegistry) {
        Preconditions.checkArgument(minecraft != null);

        net.minecraft.core.Registry<M> registry = getMinecraftRegistry(registryKey);
        B bukkit = bukkitRegistry.get(CraftNamespacedKey.fromMinecraft(registry.getResourceKey(minecraft)
                .orElseThrow(() -> new IllegalStateException(String.format(
                        "Cannot convert '%s' to bukkit representation, since it is not registered.",
                        minecraft))).location()));

        Preconditions.checkArgument(bukkit != null);

        return bukkit;
    }

    @SuppressWarnings("unchecked")
    public static <B extends Keyed, M> M bukkitToMinecraft(B bukkit) {
        Preconditions.checkArgument(bukkit != null);

        return ((Handleable<M>) bukkit).getHandle();
    }

    public static <B extends Keyed, M> Holder<M> bukkitToMinecraftHolder(B bukkit,
            ResourceKey<net.minecraft.core.Registry<M>> registryKey) {
        Preconditions.checkArgument(bukkit != null);

        net.minecraft.core.Registry<M> registry = getMinecraftRegistry(registryKey);

        if (registry.wrapAsHolder(CraftRegistry.<B, M>bukkitToMinecraft(bukkit)) instanceof Holder.Reference<M> holder) {
            return holder;
        }

        throw new IllegalArgumentException("No Reference holder found for " + bukkit
                + ", this can happen if a plugin creates its own registry entry with out properly registering it.");
    }

    public static <T extends Keyed, M> Optional<T> unwrapAndConvertHolder(
            io.papermc.paper.registry.RegistryKey<T> registryKey, Holder<M> value) {
        return unwrapAndConvertHolder(
                io.papermc.paper.registry.RegistryAccess.registryAccess().getRegistry(registryKey), value);
    }

    public static <T extends Keyed, M> Optional<T> unwrapAndConvertHolder(Registry<T> registry, Holder<M> value) {
        return value.unwrapKey().map(key -> registry.get(CraftNamespacedKey.fromMinecraft(key.location())));
    }

    /**
     * Registry lookup that first replays the renames a key went through since {@code apiVersion}.
     *
     * <p>Only meaningful for ConfigurationSerializable round-trips, where a key was written by an
     * older server and has to be read back now.
     */
    public static <B extends Keyed> B get(Registry<B> bukkit, NamespacedKey namespacedKey, ApiVersion apiVersion) {
        if (bukkit instanceof Registry.SimpleRegistry<?> simple) {
            Class<?> type = simple.getType();

            if (type == Biome.class) {
                return bukkit.get(FieldRename.BIOME_RENAME.apply(namespacedKey, apiVersion));
            }

            if (type == EntityType.class) {
                return bukkit.get(FieldRename.ENTITY_TYPE_RENAME.apply(namespacedKey, apiVersion));
            }

            if (type == Particle.class) {
                return bukkit.get(FieldRename.PARTICLE_TYPE_RENAME.apply(namespacedKey, apiVersion));
            }

            if (type == Attribute.class) {
                return bukkit.get(FieldRename.ATTRIBUTE_RENAME.apply(namespacedKey, apiVersion));
            }
        }

        return bukkit.get(namespacedKey);
    }
}
