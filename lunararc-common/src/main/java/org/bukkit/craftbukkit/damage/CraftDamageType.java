package org.bukkit.craftbukkit.damage;

import org.bukkit.NamespacedKey;
import org.bukkit.damage.DamageEffect;
import org.bukkit.damage.DamageScaling;
import org.bukkit.damage.DamageType;
import org.bukkit.damage.DeathMessageType;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/** Bukkit damage type backed by the live dynamic Minecraft damage-type registry. */
public final class CraftDamageType implements DamageType {
    private final NamespacedKey key;
    private final net.minecraft.world.damagesource.DamageType handle;

    public CraftDamageType(@NotNull NamespacedKey key, @NotNull net.minecraft.world.damagesource.DamageType handle) {
        this.key = Objects.requireNonNull(key, "key");
        this.handle = Objects.requireNonNull(handle, "handle");
    }

    public @NotNull net.minecraft.world.damagesource.DamageType getHandle() { return this.handle; }
    @Override public @NotNull NamespacedKey getKey() { return this.key; }
    @Override public @NotNull String getTranslationKey() { return this.handle.msgId(); }
    public @NotNull String translationKey() { return this.getTranslationKey(); }
    @Override public @NotNull DamageScaling getDamageScaling() {
        return switch (this.handle.scaling()) {
            case ALWAYS -> DamageScaling.ALWAYS;
            case WHEN_CAUSED_BY_LIVING_NON_PLAYER -> DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER;
            case NEVER -> DamageScaling.NEVER;
        };
    }
    @Override public @NotNull DamageEffect getDamageEffect() { return new CraftDamageEffect(this.handle.effects()); }
    @Override public @NotNull DeathMessageType getDeathMessageType() {
        return switch (this.handle.deathMessageType()) {
            case DEFAULT -> DeathMessageType.DEFAULT;
            case FALL_VARIANTS -> DeathMessageType.FALL_VARIANTS;
            case INTENTIONAL_GAME_DESIGN -> DeathMessageType.INTENTIONAL_GAME_DESIGN;
        };
    }
    @Override public float getExhaustion() { return this.handle.exhaustion(); }
    @Override public String toString() { return "CraftDamageType[" + this.key + "]"; }
}
