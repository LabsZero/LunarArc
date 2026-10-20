package org.bukkit.craftbukkit.inventory;

import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.LodestoneTracker;
import org.bukkit.Location;
import org.bukkit.inventory.meta.CompassMeta;
import org.jetbrains.annotations.Nullable;

/** Compass metadata backed by the LODESTONE_TRACKER data component. */
@org.bukkit.configuration.serialization.DelegateDeserialization(SerializableMeta.class)
public final class CraftMetaCompass extends CraftItemMeta implements CompassMeta {
    private LodestoneTracker tracker;

    public CraftMetaCompass() { super(); }

    public CraftMetaCompass(ItemStack nms) {
        super(nms);
        if (nms != null && !nms.isEmpty()) this.tracker = nms.get(DataComponents.LODESTONE_TRACKER);
    }

    @Override
    public void applyToNms(ItemStack nms) {
        super.applyToNms(nms);
        if (tracker == null) nms.remove(DataComponents.LODESTONE_TRACKER);
        else nms.set(DataComponents.LODESTONE_TRACKER, tracker);
    }

    @Override public boolean hasLodestone() { return tracker != null && tracker.target().isPresent(); }

    @Override
    public @Nullable Location getLodestone() {
        if (tracker == null || tracker.target().isEmpty()) return null;
        return org.bukkit.craftbukkit.util.CraftLocation.fromGlobalPos(tracker.target().get());
    }

    @Override
    public void setLodestone(@Nullable Location lodestone) {
        boolean tracked = tracker == null || tracker.tracked();
        if (lodestone == null) {
            tracker = new LodestoneTracker(Optional.empty(), tracked);
            return;
        }
        com.google.common.base.Preconditions.checkArgument(lodestone.getWorld() != null, "world is null");
        tracker = new LodestoneTracker(Optional.of(org.bukkit.craftbukkit.util.CraftLocation.toGlobalPos(lodestone)), tracked);
    }

    @Override public boolean isLodestoneTracked() { return tracker != null && tracker.tracked(); }

    @Override
    public void setLodestoneTracked(boolean tracked) {
        tracker = new LodestoneTracker(tracker == null ? Optional.<GlobalPos>empty() : tracker.target(), tracked);
    }

    @Override public boolean isLodestoneCompass() { return tracker != null; }
    @Override public void clearLodestone() { tracker = null; }

    @Override public CraftMetaCompass clone() { return (CraftMetaCompass) super.clone(); }
}
