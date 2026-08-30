package org.bukkit.craftbukkit.potion;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import org.bukkit.NamespacedKey;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.List;
import java.util.Objects;

/**
 * 1.21.1 legacy/internal PotionType data backed by the live potion registry.
 */
public final class CraftPotionType implements PotionType.InternalPotionData {
    private final NamespacedKey key;
    private final Potion handle;

    public CraftPotionType(NamespacedKey key) {
        this.key = Objects.requireNonNull(key, "key");
        var server = io.ampznetwork.lunararc.common.mod.server.LunarArcServer.minecraftServer();
        if (server == null) throw new IllegalStateException("MinecraftServer has not been attached to LunarArc yet");
        var registry = server.registryAccess().registryOrThrow(Registries.POTION);
        ResourceLocation id = ResourceLocation.parse(key.toString());
        this.handle = registry.getOptional(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown potion type " + key));
    }

    public Potion getHandle() {
        return this.handle;
    }

    @Override
    public PotionEffectType getEffectType() {
        List<PotionEffect> effects = getPotionEffects();
        return effects.isEmpty() ? null : effects.getFirst().getType();
    }

    @Override
    public List<PotionEffect> getPotionEffects() {
        return this.handle.getEffects().stream().map(CraftPotionType::toBukkit).toList();
    }

    @Override
    public boolean isInstant() {
        return this.handle.hasInstantEffects();
    }

    @Override
    public boolean isUpgradeable() {
        return hasSibling("strong_");
    }

    @Override
    public boolean isExtendable() {
        return hasSibling("long_");
    }

    @Override
    public int getMaxLevel() {
        return isUpgradeable() ? 2 : 1;
    }

    private boolean hasSibling(String prefix) {
        var server = io.ampznetwork.lunararc.common.mod.server.LunarArcServer.minecraftServer();
        if (server == null) return false;
        var registry = server.registryAccess().registryOrThrow(Registries.POTION);
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(this.key.getNamespace(), prefix + this.key.getKey());
        return registry.containsKey(id);
    }

    private static PotionEffect toBukkit(MobEffectInstance effect) {
        ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value());
        if (id == null) throw new IllegalStateException("Unregistered potion effect " + effect.getEffect());
        PotionEffectType type = PotionEffectType.getByKey(new NamespacedKey(id.getNamespace(), id.getPath()));
        if (type == null) throw new IllegalStateException("No Bukkit PotionEffectType for " + id);
        return new PotionEffect(type, effect.getDuration(), effect.getAmplifier(), effect.isAmbient(), effect.isVisible(), effect.showIcon());
    }
}
