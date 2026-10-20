package org.bukkit.craftbukkit.inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.ChargedProjectiles;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.jetbrains.annotations.NotNull;

/** Crossbow metadata backed by the CHARGED_PROJECTILES data component. */
@org.bukkit.configuration.serialization.DelegateDeserialization(SerializableMeta.class)
public final class CraftMetaCrossbow extends CraftItemMeta implements CrossbowMeta {
    private List<org.bukkit.inventory.ItemStack> projectiles = new ArrayList<>();

    public CraftMetaCrossbow() { super(); }

    public CraftMetaCrossbow(ItemStack nms) {
        super(nms);
        if (nms == null || nms.isEmpty()) return;
        ChargedProjectiles charged = nms.get(DataComponents.CHARGED_PROJECTILES);
        if (charged == null) return;
        charged.itemCopies().forEach(copy -> projectiles.add(CraftItemStack.asBukkitCopy(copy)));
    }

    @Override
    public void applyToNms(ItemStack nms) {
        super.applyToNms(nms);
        if (projectiles.isEmpty()) {
            nms.remove(DataComponents.CHARGED_PROJECTILES);
            return;
        }
        List<ItemStackTemplate> templates = new ArrayList<>(projectiles.size());
        for (org.bukkit.inventory.ItemStack item : projectiles) templates.add(CraftItemStack.asTemplate(item));
        nms.set(DataComponents.CHARGED_PROJECTILES, new ChargedProjectiles(templates));
    }

    @Override public boolean hasChargedProjectiles() { return !projectiles.isEmpty(); }
    @Override public @NotNull List<org.bukkit.inventory.ItemStack> getChargedProjectiles() { return projectiles.stream().map(org.bukkit.inventory.ItemStack::clone).toList(); }

    @Override
    public void setChargedProjectiles(List<org.bukkit.inventory.ItemStack> projectiles) {
        this.projectiles = new ArrayList<>();
        if (projectiles != null) for (org.bukkit.inventory.ItemStack item : projectiles) addChargedProjectile(item);
    }

    @Override
    public void addChargedProjectile(@NotNull org.bukkit.inventory.ItemStack item) {
        Objects.requireNonNull(item, "item");
        com.google.common.base.Preconditions.checkArgument(item.getType() == org.bukkit.Material.FIREWORK_ROCKET || item.getType().name().endsWith("ARROW"),
                "Item %s is not an arrow or firework rocket", item.getType());
        projectiles.add(item.clone());
    }

    @Override
    public CraftMetaCrossbow clone() {
        CraftMetaCrossbow clone = (CraftMetaCrossbow) super.clone();
        clone.projectiles = new ArrayList<>(projectiles);
        return clone;
    }
}
