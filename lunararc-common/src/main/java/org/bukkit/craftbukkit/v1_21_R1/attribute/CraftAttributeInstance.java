package org.bukkit.craftbukkit.v1_21_R1.attribute;

import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

public class CraftAttributeInstance implements org.bukkit.attribute.AttributeInstance {

    private final AttributeInstance handle;
    private final Attribute attribute;

    public CraftAttributeInstance(AttributeInstance handle, Attribute attribute) {
        this.handle = handle;
        this.attribute = attribute;
    }

    public AttributeInstance getHandle() {
        return handle;
    }

    @Override
    public @NotNull Attribute getAttribute() {
        return attribute;
    }

    @Override
    public double getBaseValue() {
        return handle.getBaseValue();
    }

    @Override
    public void setBaseValue(double value) {
        handle.setBaseValue(value);
    }

    @Override
    public double getValue() {
        return handle.getValue();
    }

    @Override
    public double getDefaultValue() {
        return handle.getAttribute().value().getDefaultValue();
    }

    @Override
    public @NotNull Collection<AttributeModifier> getModifiers() {
        return Collections.emptyList();
    }

    @Override
    public void addModifier(@NotNull AttributeModifier modifier) {}

    @Override
    public void removeModifier(@NotNull AttributeModifier modifier) {}

    @Override
    public @Nullable AttributeModifier getModifier(@NotNull UUID id) {
        return null;
    }

    @Override
    public void removeModifier(@NotNull UUID id) {}

    @Override
    public void addTransientModifier(@NotNull AttributeModifier modifier) {}
}
