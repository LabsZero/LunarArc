package org.bukkit.craftbukkit.v1_21_R1.inventory.tags;

import org.bukkit.inventory.meta.tags.ItemTagType;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public final class DeprecatedItemTagType<P,C> implements PersistentDataType<P,C> {
    private final ItemTagType<P,C> delegate;
    public DeprecatedItemTagType(ItemTagType<P,C> delegate) { this.delegate = delegate; }
    @Override public @NotNull Class<P> getPrimitiveType() { return delegate.getPrimitiveType(); }
    @Override public @NotNull Class<C> getComplexType() { return delegate.getComplexType(); }
    @Override public @NotNull P toPrimitive(@NotNull C complex, @NotNull PersistentDataAdapterContext context) { return delegate.toPrimitive(complex, new DeprecatedItemAdapterContext(context)); }
    @Override public @NotNull C fromPrimitive(@NotNull P primitive, @NotNull PersistentDataAdapterContext context) { return delegate.fromPrimitive(primitive, new DeprecatedItemAdapterContext(context)); }
}
