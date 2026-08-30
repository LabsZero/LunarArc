package io.papermc.paper.attribute;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import org.bukkit.attribute.Attributable;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Read-only Bukkit view over Minecraft's real default AttributeSupplier. */
public final class UnmodifiableAttributeMap implements Attributable {
    private final AttributeSupplier handle;

    public UnmodifiableAttributeMap(@NotNull AttributeSupplier handle) {
        this.handle = java.util.Objects.requireNonNull(handle, "handle");
    }

    @Override
    public @Nullable AttributeInstance getAttribute(@NotNull Attribute attribute) {
        java.util.Objects.requireNonNull(attribute, "attribute");
        ResourceLocation id = ResourceLocation.tryParse(attribute.getKey().toString());
        if (id == null) return null;
        var nmsAttribute = BuiltInRegistries.ATTRIBUTE.get(id);
        if (nmsAttribute == null) return null;
        var holder = BuiltInRegistries.ATTRIBUTE.wrapAsHolder(nmsAttribute);
        if (!this.handle.hasAttribute(holder)) return null;
        net.minecraft.world.entity.ai.attributes.AttributeInstance instance = this.handle.getAttributeInstance(holder);
        return instance == null ? null : new UnmodifiableAttributeInstance(instance, attribute);
    }

    @Override
    public void registerAttribute(@NotNull Attribute attribute) {
        throw new UnsupportedOperationException("Cannot register attributes on the default entity attribute map");
    }
}
