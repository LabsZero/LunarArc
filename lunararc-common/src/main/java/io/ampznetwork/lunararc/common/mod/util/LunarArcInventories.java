package io.ampznetwork.lunararc.common.mod.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * The Bukkit {@link Inventory} for an NMS {@link Container}, where one can be named.
 *
 * <p>CraftBukkit gets this by patching {@code Container} itself, adding {@code getOwner} and
 * {@code getOwnerInventory} as default methods. We cannot patch the interface - the Minecraft
 * runtime belongs to the loader - so the same decision is made from outside, on the same inputs
 * and in the same order.</p>
 *
 * <p>Events that name an inventory are only as useful as the object they hand over. A plugin
 * receiving an {@code InventoryMoveItemEvent} will cast the destination to {@code Chest} or call
 * {@code setItem} on it, so producing something that merely implements Inventory is not a smaller
 * version of the feature - it is an event that misleads. That is why this resolves through the
 * block state, and why it answers null rather than improvising when it cannot.</p>
 *
 * <p>A container that is a block entity resolves to its Bukkit block state, and if that state is
 * an {@code InventoryHolder} - chest, furnace, barrel, hopper, dropper, dispenser, brewing stand
 * and the rest - its inventory is the answer. The state is asked for without a snapshot, as Paper
 * asks for it, because an event carrying a snapshot is an event whose setItem quietly does
 * nothing.</p>
 *
 * <p><strong>Two cases deliberately answer null rather than guess.</strong> CraftBukkit's fallback
 * for both is {@code new CraftInventory(container)} - a live view over the NMS container - and
 * this project's CraftInventory is not that. It is a standalone ItemStack array with no container
 * behind it and no constructor taking one, so the nearest thing available here would be a detached
 * copy: a plugin would receive an inventory whose setItem changed nothing and whose contents
 * stopped tracking the block the moment it was handed over. Declining to fire is the honest
 * answer until CraftInventory is backed by a container the way CraftBukkit's is.</p>
 * <ul>
 *   <li>A double chest, which is a {@code CompoundContainer} and not a block entity, so no block
 *       state exists for it. CraftBukkit uses CraftInventoryDoubleChest, whose constructor chains
 *       into the container-taking CraftInventory constructor we do not have - calling it would
 *       throw NoSuchMethodError at the moment a hopper first touched a large chest.</li>
 *   <li>A mod's own container with no Bukkit block state behind it.</li>
 * </ul>
 */
public final class LunarArcInventories {

    private LunarArcInventories() {
    }

    /** The Bukkit inventory a container belongs to, or null when none can be named for it. */
    public static Inventory ownerInventory(Container container) {
        InventoryHolder owner = owner(container);
        return owner != null ? owner.getInventory() : null;
    }

    /** The Bukkit block state that owns this container, or null when nothing does. */
    public static InventoryHolder owner(Container container) {
        if (!(container instanceof BlockEntity blockEntity)) return null;
        if (!(blockEntity.getLevel() instanceof ServerLevel level)) return null;
        // A level a mod is simulating has no world a plugin has seen, so there is no owner to name.
        if (!LunarArcLogicWorlds.isLogicWorld(level)) return null;

        BlockPos position = blockEntity.getBlockPos();
        // getState(false) rather than getState(): a plain getState() is a snapshot copy, and an
        // event carrying a copy is an event whose setItem does nothing. Paper asks for the same
        // thing at the same point, as getOwner(false).
        org.bukkit.block.BlockState state = CraftBlock.at(level, position).getState(false);
        return state instanceof InventoryHolder holder ? holder : null;
    }
}
