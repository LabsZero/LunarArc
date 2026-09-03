package org.bukkit.craftbukkit.inventory;

import net.minecraft.world.Container;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.inventory.AbstractHorseInventory;
import org.bukkit.inventory.ItemStack;

/** Live combined horse inventory backed by the horse's real NMS containers. */
public class CraftInventoryAbstractHorse extends CraftNMSInventory implements AbstractHorseInventory {
    private final Container bodyArmor;

    public CraftInventoryAbstractHorse(Container main, Container bodyArmor, AbstractHorse owner) {
        super(main, owner);
        this.bodyArmor = java.util.Objects.requireNonNull(bodyArmor, "bodyArmor");
    }

    protected Container main() { return getHandle(); }
    protected Container armor() { return bodyArmor; }

    @Override public int getSize() { return main().getContainerSize() + bodyArmor.getContainerSize(); }
    @Override public boolean isEmpty() { return main().isEmpty() && bodyArmor.isEmpty(); }

    @Override public ItemStack getSaddle() { return getItem(net.minecraft.world.entity.animal.horse.AbstractHorse.INV_SLOT_SADDLE); }
    @Override public void setSaddle(ItemStack stack) { setItem(net.minecraft.world.entity.animal.horse.AbstractHorse.INV_SLOT_SADDLE, stack); }

    public ItemStack getArmor() { return getItem(1); }
    public void setArmor(ItemStack stack) { setItem(1, stack); }

    @Override public ItemStack getItem(int index) {
        if (index < 0 || index >= getSize()) return null;
        net.minecraft.world.item.ItemStack nms;
        if (index == 1) nms = bodyArmor.getItem(0);
        else nms = main().getItem(index > 1 ? index - 1 : index);
        return nms.isEmpty() ? null : CraftItemStack.asBukkitCopy(nms);
    }

    @Override public void setItem(int index, ItemStack item) {
        if (index < 0 || index >= getSize()) return;
        net.minecraft.world.item.ItemStack nms = CraftItemStack.asNMSCopy(item);
        if (index == 1) {
            bodyArmor.setItem(0, nms);
            bodyArmor.setChanged();
        } else {
            main().setItem(index > 1 ? index - 1 : index, nms);
            main().setChanged();
        }
    }

    @Override public void clear() {
        main().clearContent();
        bodyArmor.clearContent();
        main().setChanged();
        bodyArmor.setChanged();
    }
}
