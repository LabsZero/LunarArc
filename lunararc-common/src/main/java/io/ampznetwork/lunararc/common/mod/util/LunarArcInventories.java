package io.ampznetwork.lunararc.common.mod.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.inventory.CraftInventory;
import org.bukkit.craftbukkit.inventory.CraftInventoryDoubleChest;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * The Bukkit {@link Inventory} for an NMS {@link Container}.
 *
 * <p>CraftBukkit gets this by patching {@code Container} itself, adding {@code getOwner()} and
 * {@code getOwnerInventory()} as default methods and overriding them per block entity. We cannot
 * patch the interface - the Minecraft runtime belongs to the loader - so the same decision is made
 * from the outside here, on the same inputs and in the same order.</p>
 *
 * <p>Events that name an inventory are only as useful as the object they hand over. A plugin
 * receiving an {@code InventoryMoveItemEvent} will cast the destination to {@code Chest} or call
 * {@code setItem} on it, so producing a plain wrapper where a typed, live inventory was expected is
 * not a smaller version of the feature - it is an event that misleads. That is why this resolves
 * through the block state rather than wrapping the container directly whenever it can.</p>
 *
 * <p>The three cases, in the order CraftBukkit takes them:</p>
 * <ul>
 *   <li>A double chest is a {@code CompoundContainer} and is not a block entity at all, so no block
 *       state can be found for it. It gets the double-chest inventory directly, which is what makes
 *       the two halves read as one to a plugin.</li>
 *   <li>A container that is a block entity resolves to its Bukkit block state, and if that state is
 *       an {@code InventoryHolder} - chest, furnace, barrel, hopper, dispenser and the rest - its
 *       inventory is the answer. The state is a placed one, so its {@code getInventory()} returns
 *       the live inventory rather than the snapshot {@code getBlockInventory()} would give.</li>
 *   <li>Anything else - a mod's own container, a container with no block behind it - is wrapped
 *       live. This is CraftBukkit's own fallback, and it keeps the event honest about a modded
 *       inventory instead of dropping it.</li>
 * </ul>
 */
public final class LunarArcInventories {

    private LunarArcInventories() {
    }

    /** The Bukkit inventory a container belongs to, or a live wrapper when nothing owns it. */
    public static Inventory ownerInventory(Container container) {
        if (container == null) return null;
        if (container instanceof CompoundContainer compound) {
            return new CraftInventoryDoubleChest(compound);
        }
        InventoryHolder owner = owner(container);
        return owner != null ? owner.getInventory() : new CraftInventory(container);
    }

    /** The Bukkit block state that owns this container, or null when nothing does. */
    public static InventoryHolder owner(Container container) {
        if (!(container instanceof BlockEntity blockEntity)) return null;
        if (!(blockEntity.getLevel() instanceof ServerLevel level)) return null;
        // A level a mod is simulating has no world a plugin has seen, so there is no owner to name.
        if (!LunarArcLogicWorlds.isLogicWorld(level)) return null;

        BlockPos position = blockEntity.getBlockPos();
        // getState(false) rather than getState(): the snapshot a plain getState() gives is a copy,
        // and an event carrying a copy is an event whose setItem does nothing. Paper asks for the
        // same thing at the same point, as getOwner(false).
        org.bukkit.block.BlockState state = CraftBlock.at(level, position).getState(false);
        return state instanceof InventoryHolder holder ? holder : null;
    }
}
