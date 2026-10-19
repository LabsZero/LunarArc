package io.lunararcdevs.lunararc.common.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;

/** Per-tick furnace context shared by the furnace mixins (the statics of a mixin class are private to that class). */
public final class LunarArcFurnaceState {
    public static ServerLevel level;
    public static BlockPos pos;
    public static FurnaceBurnEvent burnEvent;

    private LunarArcFurnaceState() {}

    /**
     * Fires FurnaceSmeltEvent for a smelt that is about to happen.
     *
     * @return the stack to smelt into, or null when a plugin cancelled (or the changed result cannot merge into the output slot)
     */
    public static ItemStack smeltResult(NonNullList<ItemStack> slots, ItemStack source, ItemStack result) {
        if (level == null) return result;
        org.bukkit.inventory.ItemStack bukkitResult = CraftItemStack.asBukkitCopy(result);
        FurnaceSmeltEvent event = new FurnaceSmeltEvent(CraftBlock.at(level, pos), CraftItemStack.asCraftMirror(source), bukkitResult, null);
        org.bukkit.Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return null;
        if (event.getResult().equals(bukkitResult)) return result;
        ItemStack changed = CraftItemStack.asNMSCopy(event.getResult());
        ItemStack existing = slots.get(2);
        if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, changed)) return null;
        return changed;
    }
}
