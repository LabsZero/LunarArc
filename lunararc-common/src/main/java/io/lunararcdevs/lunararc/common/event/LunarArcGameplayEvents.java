package io.lunararcdevs.lunararc.common.event;

import io.lunararcdevs.lunararc.common.LunarArcServerAccess;
import io.lunararcdevs.lunararc.common.bridge.AbstractContainerMenuBridge;
import io.lunararcdevs.lunararc.common.bridge.EntityBridge;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.inventory.CraftInventory;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Paper gameplay events added alongside the server-side set: collisions, golem construction, age locking, bundles, lunging, item sorting. */
public final class LunarArcGameplayEvents {
    private LunarArcGameplayEvents() {
    }

    private static boolean listened(HandlerList handlers) {
        return handlers.getRegisteredListeners().length != 0;
    }

    private static boolean call(Event event) {
        LunarArcServerAccess.getCraftServer().getPluginManager().callEvent(event);
        return !(event instanceof Cancellable cancellable) || !cancellable.isCancelled();
    }

    private static org.bukkit.entity.Entity bukkit(Entity entity) {
        return entity != null && ((EntityBridge) entity).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Entity found ? found : null;
    }

    /** EntityCollideWithEntityEvent. @return false when a plugin cancelled the collision. */
    public static boolean collide(Entity self, Entity other) {
        if (!listened(io.papermc.paper.event.entity.EntityCollideWithEntityEvent.getHandlerList())) return true;
        org.bukkit.entity.Entity a = bukkit(self);
        org.bukkit.entity.Entity b = bukkit(other);
        if (a == null || b == null) return true;
        return call(new io.papermc.paper.event.entity.EntityCollideWithEntityEvent(a, b));
    }

    /** EntityConstructEvent for a built golem or wither. @return false when a plugin cancelled the construction. */
    public static boolean construct(Level level, Entity built, BlockPattern.BlockPatternMatch match) {
        if (!listened(io.papermc.paper.event.entity.EntityConstructEvent.getHandlerList()) || !(level instanceof net.minecraft.server.level.ServerLevel)) return true;
        org.bukkit.entity.Entity entity = bukkit(built);
        if (entity == null) return true;
        List<org.bukkit.block.Block> blocks = new ArrayList<>();
        for (int x = 0; x < match.getWidth(); x++) {
            for (int y = 0; y < match.getHeight(); y++) {
                for (int z = 0; z < match.getDepth(); z++) {
                    blocks.add(CraftBlock.at(level, match.getBlock(x, y, z).getPos()));
                }
            }
        }
        return call(new io.papermc.paper.event.entity.EntityConstructEvent(entity, blocks));
    }

    /** PlayerToggleEntityAgeLockEvent. @return false when cancelled; the client is resynced like Paper does. */
    public static boolean toggleAgeLock(Player player, Mob target, ItemStack used, InteractionHand hand, boolean locked, EntityDataAccessor<?> accessor) {
        if (!listened(io.papermc.paper.event.player.PlayerToggleEntityAgeLockEvent.getHandlerList())) return true;
        org.bukkit.entity.Entity bukkitPlayer = bukkit(player);
        org.bukkit.entity.Entity bukkitTarget = bukkit(target);
        if (!(bukkitPlayer instanceof org.bukkit.entity.Player p) || !(bukkitTarget instanceof org.bukkit.entity.LivingEntity living)) return true;
        io.papermc.paper.event.player.PlayerToggleEntityAgeLockEvent event = new io.papermc.paper.event.player.PlayerToggleEntityAgeLockEvent(
                p, living, CraftItemStack.asBukkitCopy(used),
                hand == InteractionHand.OFF_HAND ? org.bukkit.inventory.EquipmentSlot.OFF_HAND : org.bukkit.inventory.EquipmentSlot.HAND, locked);
        if (call(event)) return true;
        if (player instanceof ServerPlayer serverPlayer) {
            if (accessor != null) resync(serverPlayer, target, accessor);
            if (!player.hasInfiniteMaterials()) serverPlayer.inventoryMenu.sendAllDataToRemote();
        }
        return false;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void resync(ServerPlayer viewer, Mob target, EntityDataAccessor accessor) {
        viewer.connection.send(new ClientboundSetEntityDataPacket(target.getId(),
                List.of(net.minecraft.network.syncher.SynchedEntityData.DataValue.create(accessor, target.getEntityData().get(accessor)))));
    }

    /** PlayerBundleItemSelectEvent: fired before the selection changes. */
    public static void bundleSelect(Player player, int slotId, int selectedIndex) {
        if (!listened(io.papermc.paper.event.inventory.PlayerBundleItemSelectEvent.getHandlerList())) return;
        // creative inventory packets always carry slot 0, which says nothing about the bundle
        if (player.isCreative() && player.containerMenu instanceof net.minecraft.world.inventory.InventoryMenu) return;
        if (slotId < 0 || slotId >= player.containerMenu.slots.size() || selectedIndex <= BundleContents.NO_SELECTED_ITEM_INDEX) return;
        ItemStack item = player.containerMenu.slots.get(slotId).getItem();
        if (!item.is(net.minecraft.tags.ItemTags.BUNDLES)) return;
        BundleContents contents = item.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null) return;
        int previousIndex = contents.getSelectedItemIndex();
        if (selectedIndex >= contents.size() || selectedIndex == previousIndex) return;

        io.papermc.paper.event.inventory.PlayerBundleItemSelectEvent.Direction direction = io.papermc.paper.event.inventory.PlayerBundleItemSelectEvent.Direction.UNKNOWN;
        // the scroll direction is only knowable with three or more stacks, from the before and after index
        if (contents.size() >= 3) {
            if (previousIndex == BundleContents.NO_SELECTED_ITEM_INDEX) {
                direction = selectedIndex == 0 ? io.papermc.paper.event.inventory.PlayerBundleItemSelectEvent.Direction.FORWARD
                        : io.papermc.paper.event.inventory.PlayerBundleItemSelectEvent.Direction.BACKWARD;
            } else {
                int diff = selectedIndex - previousIndex;
                direction = diff > 0 ? io.papermc.paper.event.inventory.PlayerBundleItemSelectEvent.Direction.FORWARD
                        : io.papermc.paper.event.inventory.PlayerBundleItemSelectEvent.Direction.BACKWARD;
                if (Math.abs(diff) == contents.size() - 1) direction = direction.flip();
            }
        }
        org.bukkit.inventory.InventoryView view = ((AbstractContainerMenuBridge) player.containerMenu).lunararc$getBukkitView();
        if (view == null) return;
        call(new io.papermc.paper.event.inventory.PlayerBundleItemSelectEvent(
                view, CraftItemStack.asBukkitMirror(item), slotId,
                contents.getSelectedItem() != null ? CraftItemStack.asBukkitMirror(contents.getSelectedItem().create()) : org.bukkit.inventory.ItemStack.empty(),
                CraftItemStack.asBukkitMirror(contents.items().get(selectedIndex).create()), previousIndex, selectedIndex, direction));
    }

    /** EntityLungeEvent. @return the lunge power to use, or -1 when a plugin cancelled the effect. */
    public static int lunge(Entity user, int power) {
        if (!listened(io.papermc.paper.event.entity.EntityLungeEvent.getHandlerList()) || !(user instanceof LivingEntity)) return power;
        if (!(bukkit(user) instanceof org.bukkit.entity.LivingEntity living)) return power;
        io.papermc.paper.event.entity.EntityLungeEvent event = new io.papermc.paper.event.entity.EntityLungeEvent(living, power);
        return call(event) ? event.getLungePower() : -1;
    }

    /** ItemTransportingEntitySortEvent. @return null to keep vanilla's answer, else whether the container is acceptable. */
    public static Boolean itemSort(PathfinderMob mob, Container container) {
        if (!listened(io.papermc.paper.event.entity.ItemTransportingEntitySortEvent.getHandlerList())) return null;
        org.bukkit.entity.Entity entity = bukkit(mob);
        if (entity == null) return null;
        io.papermc.paper.event.entity.ItemTransportingEntitySortEvent event = new io.papermc.paper.event.entity.ItemTransportingEntitySortEvent(
                entity, CraftItemStack.asBukkitCopy(mob.getMainHandItem()), new CraftInventory(container));
        call(event);
        return event.getResult() == Event.Result.DEFAULT ? null : event.getResult() == Event.Result.ALLOW;
    }

    /** EntityIgniteEvent. @return the fuse to use, or {@link net.minecraft.world.entity.item.PrimedTnt#NO_FUSE} when cancelled. */
    public static int ignite(Entity entity, int fuse) {
        if (!listened(io.papermc.paper.event.entity.EntityIgniteEvent.getHandlerList())) return fuse;
        org.bukkit.entity.Entity bukkit = bukkit(entity);
        if (bukkit == null) return fuse;
        io.papermc.paper.event.entity.EntityIgniteEvent event = new io.papermc.paper.event.entity.EntityIgniteEvent(bukkit, fuse);
        return call(event) ? event.getFuseTime() : net.minecraft.world.entity.item.PrimedTnt.NO_FUSE;
    }

    /**
     * SulfurCubeSwallowItemEvent.
     *
     * @return the stack to swallow (the same instance when a plugin changed nothing), or null when cancelled
     */
    public static ItemStack swallow(net.minecraft.world.entity.monster.cubemob.SulfurCube cube, Player player, ItemStack held, InteractionHand hand) {
        if (!listened(io.papermc.paper.event.entity.SulfurCubeSwallowItemEvent.getHandlerList())) return held;
        if (!(bukkit(cube) instanceof org.bukkit.entity.SulfurCube bukkitCube) || !(bukkit(player) instanceof org.bukkit.entity.Player bukkitPlayer)) return held;
        org.bukkit.inventory.ItemStack heldMirror = CraftItemStack.asBukkitMirror(held);
        io.papermc.paper.event.entity.SulfurCubeSwallowItemEvent event = new io.papermc.paper.event.entity.SulfurCubeSwallowItemEvent(
                bukkitCube, bukkitPlayer, CraftItemStack.asBukkitMirror(cube.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.BODY)), heldMirror);
        if (!call(event)) {
            if (player instanceof ServerPlayer serverPlayer) serverPlayer.inventoryMenu.sendAllDataToRemote();
            return null;
        }
        return heldMirror.equals(event.getNewItem()) ? held : CraftItemStack.asNMSCopy(event.getNewItem());
    }

    /** PlayerInventorySlotChangeEvent for a slot backed by the player's own inventory. */
    public static void inventorySlotChanged(net.minecraft.world.inventory.Slot slot, int rawSlot, ItemStack previous, ItemStack current) {
        if (!listened(io.papermc.paper.event.player.PlayerInventorySlotChangeEvent.getHandlerList())) return;
        if (slot instanceof net.minecraft.world.inventory.ResultSlot
                || !(slot.container instanceof net.minecraft.world.entity.player.Inventory inventory)
                || !(inventory.player instanceof ServerPlayer player)
                || !(bukkit(player) instanceof org.bukkit.entity.Player bukkitPlayer)) return;
        call(new io.papermc.paper.event.player.PlayerInventorySlotChangeEvent(bukkitPlayer, rawSlot,
                CraftItemStack.asBukkitCopy(previous), CraftItemStack.asBukkitCopy(current)));
    }

    // PlayerNaturallySpawnCreaturesEvent -------------------------------------------------------------------------------

    /** Returned by {@link #naturalSpawnRadius} when no plugin listens, so vanilla's own range applies. */
    public static final int NO_SPAWN_EVENT = Integer.MIN_VALUE;

    private static final java.util.Map<ServerPlayer, com.destroystokyo.paper.event.entity.PlayerNaturallySpawnCreaturesEvent> SPAWN_EVENTS =
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    /** Fires the event for every player of the level; the results are read back while the chunks of that tick are examined. */
    public static void fireNaturalSpawn(net.minecraft.server.level.ServerLevel level) {
        if (!listened(com.destroystokyo.paper.event.entity.PlayerNaturallySpawnCreaturesEvent.getHandlerList())) {
            SPAWN_EVENTS.clear();
            return;
        }
        for (ServerPlayer player : level.players()) {
            if (!(bukkit(player) instanceof org.bukkit.entity.Player bukkitPlayer)) continue;
            int range = Math.min(Math.min(8, level.getServer().getPlayerList().getViewDistance()), bukkitPlayer.getViewDistance());
            com.destroystokyo.paper.event.entity.PlayerNaturallySpawnCreaturesEvent event =
                    new com.destroystokyo.paper.event.entity.PlayerNaturallySpawnCreaturesEvent(bukkitPlayer, (byte) range);
            call(event);
            SPAWN_EVENTS.put(player, event);
        }
    }

    /** @return the player's spawn radius in chunks, -1 when a plugin cancelled spawning around them, or {@link #NO_SPAWN_EVENT} */
    public static int naturalSpawnRadius(ServerPlayer player) {
        com.destroystokyo.paper.event.entity.PlayerNaturallySpawnCreaturesEvent event = SPAWN_EVENTS.get(player);
        if (event == null) return NO_SPAWN_EVENT;
        return event.isCancelled() ? -1 : event.getSpawnRadius();
    }
}
