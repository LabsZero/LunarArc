package org.bukkit.craftbukkit.v1_21_R1.inventory.tags;

import org.bukkit.inventory.meta.tags.CustomItemTagContainer;
import org.bukkit.inventory.meta.tags.ItemTagType;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public final class DeprecatedContainerTagType<C> implements PersistentDataType<PersistentDataContainer,C> {
    private final ItemTagType<CustomItemTagContainer,C> delegate;
    public DeprecatedContainerTagType(ItemTagType<CustomItemTagContainer,C> delegate) { this.delegate = delegate; }
    @Override public @NotNull Class<PersistentDataContainer> getPrimitiveType() { return PersistentDataContainer.class; }
    @Override public @NotNull Class<C> getComplexType() { return delegate.getComplexType(); }
    @Override public @NotNull PersistentDataContainer toPrimitive(@NotNull C complex, @NotNull PersistentDataAdapterContext context) {
        CustomItemTagContainer old = delegate.toPrimitive(complex, new DeprecatedItemAdapterContext(context));
        if (!(old instanceof DeprecatedCustomTagContainer wrapped)) throw new IllegalArgumentException("Foreign CustomItemTagContainer implementation " + old.getClass().getName());
        PersistentDataContainer copy = context.newPersistentDataContainer();
        wrapped.getWrapped().copyTo(copy, true);
        return copy;
    }
    @Override public @NotNull C fromPrimitive(@NotNull PersistentDataContainer primitive, @NotNull PersistentDataAdapterContext context) {
        return delegate.fromPrimitive(new DeprecatedCustomTagContainer(primitive), new DeprecatedItemAdapterContext(context));
    }
}
