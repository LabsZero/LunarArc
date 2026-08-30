package io.papermc.paper.attribute;

import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.craftbukkit.attribute.CraftAttributeInstance;
import org.jetbrains.annotations.NotNull;

/** Read-only Bukkit view over an NMS default attribute instance. */
public final class UnmodifiableAttributeInstance extends CraftAttributeInstance {
    public UnmodifiableAttributeInstance(AttributeInstance handle, Attribute attribute) {
        super(handle, attribute);
    }

    private UnsupportedOperationException immutable() {
        return new UnsupportedOperationException("Cannot modify default entity attributes");
    }

    @Override public void setBaseValue(double value) { throw immutable(); }
    @Override public void addModifier(@NotNull AttributeModifier modifier) { throw immutable(); }
    @Override public void addTransientModifier(@NotNull AttributeModifier modifier) { throw immutable(); }
    @Override public void removeModifier(@NotNull AttributeModifier modifier) { throw immutable(); }
    @Override public void removeModifier(@NotNull java.util.UUID uuid) { throw immutable(); }
    @Override public void removeModifier(@NotNull net.kyori.adventure.key.Key key) { throw immutable(); }
}
