package org.bukkit.craftbukkit.inventory;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.OminousBottleAmplifier;
import org.bukkit.inventory.meta.OminousBottleMeta;

/** Ominous-bottle metadata backed by the OMINOUS_BOTTLE_AMPLIFIER data component. */
@org.bukkit.configuration.serialization.DelegateDeserialization(SerializableMeta.class)
public final class CraftMetaOminousBottle extends CraftItemMeta implements OminousBottleMeta {
    private Integer amplifier;

    public CraftMetaOminousBottle() { super(); }

    public CraftMetaOminousBottle(ItemStack nms) {
        super(nms);
        if (nms == null || nms.isEmpty()) return;
        OminousBottleAmplifier value = nms.get(DataComponents.OMINOUS_BOTTLE_AMPLIFIER);
        if (value != null) this.amplifier = value.value();
    }

    @Override
    public void applyToNms(ItemStack nms) {
        super.applyToNms(nms);
        if (amplifier == null) nms.remove(DataComponents.OMINOUS_BOTTLE_AMPLIFIER);
        else nms.set(DataComponents.OMINOUS_BOTTLE_AMPLIFIER, new OminousBottleAmplifier(amplifier));
    }

    @Override public boolean hasAmplifier() { return amplifier != null; }

    @Override
    public int getAmplifier() {
        com.google.common.base.Preconditions.checkState(amplifier != null, "Ominous bottle has no amplifier");
        return amplifier;
    }

    @Override
    public void setAmplifier(int amplifier) {
        com.google.common.base.Preconditions.checkArgument(amplifier >= OminousBottleAmplifier.MIN_AMPLIFIER && amplifier <= OminousBottleAmplifier.MAX_AMPLIFIER,
                "Amplifier must be between %s and %s", OminousBottleAmplifier.MIN_AMPLIFIER, OminousBottleAmplifier.MAX_AMPLIFIER);
        this.amplifier = amplifier;
    }

    @Override public CraftMetaOminousBottle clone() { return (CraftMetaOminousBottle) super.clone(); }
}
