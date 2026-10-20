package org.bukkit.craftbukkit.inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.BundleContents;
import org.bukkit.inventory.meta.BundleMeta;
import org.jetbrains.annotations.NotNull;

/** Bundle metadata backed by the BUNDLE_CONTENTS data component. */
@org.bukkit.configuration.serialization.DelegateDeserialization(SerializableMeta.class)
public final class CraftMetaBundle extends CraftItemMeta implements BundleMeta {
    private List<org.bukkit.inventory.ItemStack> items = new ArrayList<>();

    public CraftMetaBundle() { super(); }

    public CraftMetaBundle(ItemStack nms) {
        super(nms);
        if (nms == null || nms.isEmpty()) return;
        BundleContents contents = nms.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null) return;
        contents.itemCopies().forEach(copy -> items.add(CraftItemStack.asBukkitCopy(copy)));
    }

    @Override
    public void applyToNms(ItemStack nms) {
        super.applyToNms(nms);
        if (items.isEmpty()) {
            nms.remove(DataComponents.BUNDLE_CONTENTS);
            return;
        }
        List<ItemStackTemplate> templates = new ArrayList<>(items.size());
        for (org.bukkit.inventory.ItemStack item : items) templates.add(CraftItemStack.asTemplate(item));
        nms.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(templates));
    }

    @Override public boolean hasItems() { return !items.isEmpty(); }
    @Override public @NotNull List<org.bukkit.inventory.ItemStack> getItems() { return items.stream().map(org.bukkit.inventory.ItemStack::clone).toList(); }

    @Override
    public void setItems(List<org.bukkit.inventory.ItemStack> items) {
        this.items = new ArrayList<>();
        if (items != null) for (org.bukkit.inventory.ItemStack item : items) addItem(item);
    }

    @Override
    public void addItem(@NotNull org.bukkit.inventory.ItemStack item) {
        Objects.requireNonNull(item, "item");
        com.google.common.base.Preconditions.checkArgument(item.getType() != org.bukkit.Material.AIR, "item must not be air");
        items.add(item.clone());
    }

    @Override
    public CraftMetaBundle clone() {
        CraftMetaBundle clone = (CraftMetaBundle) super.clone();
        clone.items = new ArrayList<>(items);
        return clone;
    }
}
