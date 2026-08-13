package org.bukkit.craftbukkit.v1_21_R1.inventory.tags;

import org.bukkit.inventory.meta.tags.CustomItemTagContainer;
import org.bukkit.inventory.meta.tags.ItemTagAdapterContext;
import org.bukkit.persistence.PersistentDataAdapterContext;

/** Bridge for the pre-PDC ItemMeta custom-tag API. */
public final class DeprecatedItemAdapterContext implements ItemTagAdapterContext {
    private final PersistentDataAdapterContext context;
    public DeprecatedItemAdapterContext(PersistentDataAdapterContext context) { this.context = context; }
    @Override public CustomItemTagContainer newTagContainer() {
        return new DeprecatedCustomTagContainer(context.newPersistentDataContainer());
    }
}
