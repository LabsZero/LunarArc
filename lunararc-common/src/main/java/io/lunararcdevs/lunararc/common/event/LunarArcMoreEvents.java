package io.lunararcdevs.lunararc.common.event;

import io.lunararcdevs.lunararc.common.LunarArcServerAccess;
import io.lunararcdevs.lunararc.common.bridge.EntityBridge;
import io.papermc.paper.event.block.BeaconActivatedEvent;
import io.papermc.paper.event.block.BeaconDeactivatedEvent;
import io.papermc.paper.event.block.BellRevealRaiderEvent;
import io.papermc.paper.event.block.BlockFailedDispenseEvent;
import io.papermc.paper.event.block.BlockPreDispenseEvent;
import io.papermc.paper.event.block.DragonEggFormEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.inventory.CraftInventory;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BellResonateEvent;
import org.bukkit.event.inventory.HopperInventorySearchEvent;
import org.bukkit.event.raid.RaidFinishEvent;
import org.bukkit.event.raid.RaidSpawnWaveEvent;
import org.bukkit.event.raid.RaidStopEvent;
import org.bukkit.event.raid.RaidTriggerEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class LunarArcMoreEvents {
    private LunarArcMoreEvents() {}

    private static boolean listened(HandlerList handlers) {
        return handlers.getRegisteredListeners().length != 0;
    }

    private static boolean fire(Event event) {
        CraftServer server = LunarArcServerAccess.getCraftServer();
        server.getPluginManager().callEvent(event);
        return event instanceof Cancellable cancellable && cancellable.isCancelled();
    }

    private static boolean ready(Level level) {
        return level instanceof ServerLevel && org.bukkit.Bukkit.isPrimaryThread();
    }

    private static org.bukkit.World world(Level level) {
        return level instanceof ServerLevel serverLevel ? LunarArcServerAccess.getCraftWorld(serverLevel) : null;
    }

    private static org.bukkit.entity.Entity bukkit(net.minecraft.world.entity.Entity entity) {
        return entity != null && ((EntityBridge) entity).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Entity found ? found : null;
    }

    public static void raidStop(Raid raid, ServerLevel level, RaidStopEvent.Reason reason) {
        if (!listened(RaidStopEvent.getHandlerList()) || !ready(level)) return;
        fire(new RaidStopEvent(new org.bukkit.craftbukkit.CraftRaid(raid), world(level), reason));
    }

    public static void raidFinish(Raid raid, ServerLevel level, Collection<java.util.UUID> heroes) {
        if (!listened(RaidFinishEvent.getHandlerList()) || !ready(level)) return;
        List<org.bukkit.entity.Player> winners = new ArrayList<>();
        for (java.util.UUID id : heroes) {
            net.minecraft.world.entity.player.Player player = level.getPlayerByUUID(id);
            if (bukkit(player) instanceof org.bukkit.entity.Player found) winners.add(found);
        }
        fire(new RaidFinishEvent(new org.bukkit.craftbukkit.CraftRaid(raid), world(level), winners));
    }

    public static void raidWave(Raid raid, ServerLevel level, Raider leader, Collection<Raider> raiders) {
        if (!listened(RaidSpawnWaveEvent.getHandlerList()) || !ready(level) || raiders == null) return;
        List<org.bukkit.entity.Raider> list = new ArrayList<>();
        for (Raider raider : raiders) if (bukkit(raider) instanceof org.bukkit.entity.Raider found) list.add(found);
        fire(new RaidSpawnWaveEvent(new org.bukkit.craftbukkit.CraftRaid(raid), world(level),
                bukkit(leader) instanceof org.bukkit.entity.Raider found ? found : null, list));
    }

    public static boolean raidTrigger(Raid raid, ServerLevel level, net.minecraft.server.level.ServerPlayer player) {
        if (!listened(RaidTriggerEvent.getHandlerList()) || !ready(level) || !(bukkit(player) instanceof org.bukkit.entity.Player found)) return true;
        return !fire(new RaidTriggerEvent(new org.bukkit.craftbukkit.CraftRaid(raid), world(level), found));
    }

    public static void beacon(Level level, BlockPos pos, int before, int after) {
        if (before == after || (before > 0) == (after > 0) || !ready(level)) return;
        if (after > 0 && listened(BeaconActivatedEvent.getHandlerList())) fire(new BeaconActivatedEvent(CraftBlock.at((ServerLevel) level, pos)));
        else if (after <= 0 && listened(BeaconDeactivatedEvent.getHandlerList())) fire(new BeaconDeactivatedEvent(CraftBlock.at((ServerLevel) level, pos)));
    }

    public static boolean bellListened() {
        return listened(BellResonateEvent.getHandlerList()) || listened(BellRevealRaiderEvent.getHandlerList());
    }

    public static List<LivingEntity> bellResonate(Level level, BlockPos pos, List<LivingEntity> raiders) {
        if (!(level instanceof ServerLevel serverLevel) || !org.bukkit.Bukkit.isPrimaryThread()) return raiders;
        List<org.bukkit.entity.LivingEntity> bukkitRaiders = new ArrayList<>();
        for (LivingEntity raider : raiders) if (bukkit(raider) instanceof org.bukkit.entity.LivingEntity found) bukkitRaiders.add(found);
        BellResonateEvent event = new BellResonateEvent(CraftBlock.at(serverLevel, pos), bukkitRaiders);
        if (listened(BellResonateEvent.getHandlerList())) fire(event);
        List<LivingEntity> result = new ArrayList<>();
        for (LivingEntity raider : raiders) {
            org.bukkit.entity.Entity entity = bukkit(raider);
            if (!event.getResonatedEntities().contains(entity)) continue;
            if (entity instanceof org.bukkit.entity.Raider bukkitRaider && listened(BellRevealRaiderEvent.getHandlerList())
                    && fire(new BellRevealRaiderEvent(CraftBlock.at(serverLevel, pos), bukkitRaider))) continue;
            result.add(raider);
        }
        return result;
    }

    public static boolean dispenseAllowed(ServerLevel level, BlockPos pos, ItemStack stack) {
        if (!listened(BlockPreDispenseEvent.getHandlerList()) || !org.bukkit.Bukkit.isPrimaryThread()) return true;
        int slot = -1;
        if (level.getBlockEntity(pos) instanceof Container container) {
            for (int i = 0; i < container.getContainerSize(); i++) {
                if (container.getItem(i) == stack) {
                    slot = i;
                    break;
                }
            }
        }
        return !fire(new BlockPreDispenseEvent(CraftBlock.at(level, pos), CraftItemStack.asCraftMirror(stack), slot));
    }

    public static boolean dispenseFailed(ServerLevel level, BlockPos pos) {
        if (!listened(BlockFailedDispenseEvent.getHandlerList()) || !org.bukkit.Bukkit.isPrimaryThread()) return true;
        BlockFailedDispenseEvent event = new BlockFailedDispenseEvent(CraftBlock.at(level, pos));
        fire(event);
        return event.shouldPlayEffect();
    }

    public static boolean dragonEgg(ServerLevel level, BlockPos pos, net.minecraft.world.level.block.state.BlockState egg,
                                    net.minecraft.world.level.dimension.end.EndDragonFight fight) {
        if (!listened(DragonEggFormEvent.getHandlerList()) || !org.bukkit.Bukkit.isPrimaryThread()) return true;
        org.bukkit.craftbukkit.block.CraftBlockState state = org.bukkit.craftbukkit.block.CraftBlockStates.getBlockState(level, pos);
        state.setData(egg);
        return !fire(new DragonEggFormEvent(CraftBlock.at(level, pos), state, new org.bukkit.craftbukkit.boss.CraftDragonBattle(fight)));
    }

    public static boolean notePlay(Level level, BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        if (!listened(org.bukkit.event.block.NotePlayEvent.getHandlerList()) || !ready(level)) return true;
        if (!(org.bukkit.craftbukkit.block.data.CraftBlockData.fromData(state) instanceof org.bukkit.block.data.type.NoteBlock note)) return true;
        return !fire(new org.bukkit.event.block.NotePlayEvent(CraftBlock.at((ServerLevel) level, pos), note.getInstrument(), note.getNote()));
    }

    public static Container hopperSearch(Container inventory, Level level, BlockPos hopperPos, BlockPos searchPos, boolean destination) {
        if (!listened(HopperInventorySearchEvent.getHandlerList()) || !ready(level)) return inventory;
        ServerLevel serverLevel = (ServerLevel) level;
        HopperInventorySearchEvent event = new HopperInventorySearchEvent(inventory == null ? null : new CraftInventory(inventory),
                destination ? HopperInventorySearchEvent.ContainerType.DESTINATION : HopperInventorySearchEvent.ContainerType.SOURCE,
                CraftBlock.at(serverLevel, hopperPos), CraftBlock.at(serverLevel, searchPos));
        fire(event);
        return event.getInventory() instanceof CraftInventory craft ? craft.getInventory() : event.getInventory() == null ? null : inventory;
    }

    private static org.bukkit.entity.Player player(net.minecraft.world.entity.Entity entity) {
        return entity instanceof net.minecraft.server.level.ServerPlayer && bukkit(entity) instanceof org.bukkit.entity.Player found ? found : null;
    }

    public static boolean lecternInsert(net.minecraft.world.entity.LivingEntity who, Level level, BlockPos pos, ItemStack book) {
        org.bukkit.entity.Player player = player(who);
        if (player == null || !ready(level) || !listened(io.papermc.paper.event.player.PlayerInsertLecternBookEvent.getHandlerList())) return true;
        return !fire(new io.papermc.paper.event.player.PlayerInsertLecternBookEvent(player, CraftBlock.at((ServerLevel) level, pos),
                CraftItemStack.asCraftMirror(book.copyWithCount(1))));
    }

    public static boolean nameEntity(net.minecraft.world.entity.player.Player who, LivingEntity target, ItemStack tag) {
        org.bukkit.entity.Player player = player(who);
        if (player == null || !listened(io.papermc.paper.event.player.PlayerNameEntityEvent.getHandlerList())
                || !(bukkit(target) instanceof org.bukkit.entity.LivingEntity living)) return true;
        net.minecraft.network.chat.Component name = tag.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
        return !fire(new io.papermc.paper.event.player.PlayerNameEntityEvent(player, living,
                name == null ? null : io.papermc.paper.adventure.PaperAdventure.asAdventure(name), true));
    }

    public static boolean itemFrame(net.minecraft.world.entity.player.Player who, net.minecraft.world.entity.Entity frame, ItemStack item,
                                    io.papermc.paper.event.player.PlayerItemFrameChangeEvent.ItemFrameChangeAction action) {
        org.bukkit.entity.Player player = player(who);
        if (player == null || !listened(io.papermc.paper.event.player.PlayerItemFrameChangeEvent.getHandlerList())
                || !(bukkit(frame) instanceof org.bukkit.entity.ItemFrame bukkitFrame)) return true;
        return !fire(new io.papermc.paper.event.player.PlayerItemFrameChangeEvent(player, bukkitFrame, CraftItemStack.asBukkitCopy(item), action));
    }

    public static boolean flowerPot(net.minecraft.world.entity.player.Player who, Level level, BlockPos pos, ItemStack item) {
        org.bukkit.entity.Player player = player(who);
        if (player == null || !ready(level) || !listened(io.papermc.paper.event.player.PlayerFlowerPotManipulateEvent.getHandlerList())) return true;
        boolean allowed = !fire(new io.papermc.paper.event.player.PlayerFlowerPotManipulateEvent(player, CraftBlock.at((ServerLevel) level, pos),
                CraftItemStack.asBukkitCopy(item), true));
        if (!allowed) ((net.minecraft.server.level.ServerPlayer) who).containerMenu.sendAllDataToRemote();
        return allowed;
    }

    public static ItemStack readyArrow(net.minecraft.world.entity.player.Player who, ItemStack bow, ItemStack arrow) {
        org.bukkit.entity.Player player = player(who);
        if (player == null || arrow.isEmpty() || !listened(com.destroystokyo.paper.event.player.PlayerReadyArrowEvent.getHandlerList())) return arrow;
        return fire(new com.destroystokyo.paper.event.player.PlayerReadyArrowEvent(player, CraftItemStack.asCraftMirror(bow),
                CraftItemStack.asCraftMirror(arrow))) ? ItemStack.EMPTY : arrow;
    }

    public static boolean recipeBookClick(net.minecraft.server.level.ServerPlayer who, net.minecraft.resources.ResourceLocation recipe, boolean shift) {
        org.bukkit.entity.Player player = player(who);
        if (player == null) return true;
        org.bukkit.NamespacedKey key = org.bukkit.craftbukkit.util.CraftNamespacedKey.fromMinecraft(recipe);
        if (listened(org.bukkit.event.player.PlayerRecipeBookClickEvent.getHandlerList())) {
            org.bukkit.inventory.Recipe bukkitRecipe = org.bukkit.Bukkit.getRecipe(key);
            if (bukkitRecipe != null && fire(new org.bukkit.event.player.PlayerRecipeBookClickEvent(player, bukkitRecipe, shift))) return false;
        }
        if (!listened(com.destroystokyo.paper.event.player.PlayerRecipeBookClickEvent.getHandlerList())) return true;
        return !fire(new com.destroystokyo.paper.event.player.PlayerRecipeBookClickEvent(player, key, shift));
    }

    public static void recipeBookSettings(net.minecraft.server.level.ServerPlayer who, net.minecraft.world.inventory.RecipeBookType type,
                                          boolean open, boolean filtering) {
        org.bukkit.entity.Player player = player(who);
        if (player == null || !listened(org.bukkit.event.player.PlayerRecipeBookSettingsChangeEvent.getHandlerList())) return;
        fire(new org.bukkit.event.player.PlayerRecipeBookSettingsChangeEvent(player,
                org.bukkit.event.player.PlayerRecipeBookSettingsChangeEvent.RecipeBookType.valueOf(type.name()), open, filtering));
    }

    public static net.kyori.adventure.text.Component handshake(String hostname) {
        if (!listened(com.destroystokyo.paper.event.player.PlayerHandshakeEvent.getHandlerList())) return null;
        com.destroystokyo.paper.event.player.PlayerHandshakeEvent event = new com.destroystokyo.paper.event.player.PlayerHandshakeEvent(hostname, false);
        fire(event);
        return event.isFailed() ? event.failMessage() : null;
    }

    public static void connectionClose(net.minecraft.server.level.ServerPlayer who, java.net.SocketAddress address) {
        if (who == null || !listened(com.destroystokyo.paper.event.player.PlayerConnectionCloseEvent.getHandlerList())
                || !(address instanceof java.net.InetSocketAddress inet)) return;
        fire(new com.destroystokyo.paper.event.player.PlayerConnectionCloseEvent(who.getUUID(), who.getGameProfile().getName(), inet.getAddress(), !org.bukkit.Bukkit.isPrimaryThread()));
    }

    public static boolean setSpawn(org.bukkit.entity.Player player, org.bukkit.Location location, boolean forced, boolean notify, String cause) {
        if (location == null || !listened(com.destroystokyo.paper.event.player.PlayerSetSpawnEvent.getHandlerList())) return true;
        com.destroystokyo.paper.event.player.PlayerSetSpawnEvent.Cause mapped;
        try {
            mapped = com.destroystokyo.paper.event.player.PlayerSetSpawnEvent.Cause.valueOf(cause);
        } catch (IllegalArgumentException unknown) {
            mapped = com.destroystokyo.paper.event.player.PlayerSetSpawnEvent.Cause.UNKNOWN;
        }
        return !fire(new com.destroystokyo.paper.event.player.PlayerSetSpawnEvent(player, mapped, location, forced, notify, null));
    }

    public static boolean hideShow(org.bukkit.entity.Player viewer, org.bukkit.entity.Entity target, boolean hide) {
        if (hide && listened(org.bukkit.event.player.PlayerHideEntityEvent.getHandlerList())) {
            fire(new org.bukkit.event.player.PlayerHideEntityEvent(viewer, target));
        } else if (!hide && listened(org.bukkit.event.player.PlayerShowEntityEvent.getHandlerList())) {
            fire(new org.bukkit.event.player.PlayerShowEntityEvent(viewer, target));
        }
        return true;
    }

    private static boolean mobReady(net.minecraft.world.entity.Entity entity) {
        return entity.level() instanceof ServerLevel && org.bukkit.Bukkit.isPrimaryThread();
    }

    public static boolean sheepRegrow(net.minecraft.world.entity.Entity sheep) {
        if (!listened(org.bukkit.event.entity.SheepRegrowWoolEvent.getHandlerList()) || !mobReady(sheep)
                || !(bukkit(sheep) instanceof org.bukkit.entity.Sheep found)) return true;
        return !fire(new org.bukkit.event.entity.SheepRegrowWoolEvent(found));
    }

    public static boolean horseJump(net.minecraft.world.entity.Entity horse, int jumpPower) {
        if (!listened(org.bukkit.event.entity.HorseJumpEvent.getHandlerList()) || !mobReady(horse)
                || !(bukkit(horse) instanceof org.bukkit.entity.AbstractHorse found)) return true;
        float power = jumpPower >= 90 ? 1.0F : 0.4F + 0.4F * (float) jumpPower / 90.0F;
        return !fire(new org.bukkit.event.entity.HorseJumpEvent(found, power));
    }

    public static boolean pigZap(net.minecraft.world.entity.Entity pig, net.minecraft.world.entity.Entity bolt, net.minecraft.world.entity.Entity piglin) {
        if (!listened(org.bukkit.event.entity.PigZapEvent.getHandlerList()) || !mobReady(pig)
                || !(bukkit(pig) instanceof org.bukkit.entity.Pig bukkitPig) || !(bukkit(bolt) instanceof org.bukkit.entity.LightningStrike strike)
                || !(bukkit(piglin) instanceof org.bukkit.entity.PigZombie zombie)) return true;
        return !fire(new org.bukkit.event.entity.PigZapEvent(bukkitPig, strike, zombie));
    }

    public static List<ItemStack> piglinBarter(net.minecraft.world.entity.Entity piglin, List<ItemStack> outcome) {
        if (!listened(org.bukkit.event.entity.PiglinBarterEvent.getHandlerList()) || !mobReady(piglin)
                || !(bukkit(piglin) instanceof org.bukkit.entity.Piglin found)) return outcome;
        List<org.bukkit.inventory.ItemStack> bukkitOutcome = new ArrayList<>();
        for (ItemStack stack : outcome) bukkitOutcome.add(CraftItemStack.asBukkitCopy(stack));
        org.bukkit.event.entity.PiglinBarterEvent event = new org.bukkit.event.entity.PiglinBarterEvent(found,
                new org.bukkit.inventory.ItemStack(org.bukkit.Material.GOLD_INGOT), bukkitOutcome);
        if (fire(event)) return null;
        List<ItemStack> result = new ArrayList<>();
        for (org.bukkit.inventory.ItemStack stack : event.getOutcome()) result.add(CraftItemStack.asNMSCopy(stack));
        return result;
    }

    public static int slimeSplit(net.minecraft.world.entity.Entity slime, int count) {
        if (!listened(org.bukkit.event.entity.SlimeSplitEvent.getHandlerList()) || !mobReady(slime)
                || !(bukkit(slime) instanceof org.bukkit.entity.Slime found)) return count;
        org.bukkit.event.entity.SlimeSplitEvent event = new org.bukkit.event.entity.SlimeSplitEvent(found, count);
        return fire(event) || event.getCount() <= 0 ? 0 : event.getCount();
    }

    public static boolean slimeSwim(net.minecraft.world.entity.Entity slime) {
        if (!listened(com.destroystokyo.paper.event.entity.SlimeSwimEvent.getHandlerList()) || !mobReady(slime)
                || !(bukkit(slime) instanceof org.bukkit.entity.Slime found)) return true;
        return !fire(new com.destroystokyo.paper.event.entity.SlimeSwimEvent(found));
    }

    public static boolean slimeTarget(net.minecraft.world.entity.Entity slime, LivingEntity target) {
        if (target == null || !listened(com.destroystokyo.paper.event.entity.SlimeTargetLivingEntityEvent.getHandlerList()) || !mobReady(slime)
                || !(bukkit(slime) instanceof org.bukkit.entity.Slime found) || !(bukkit(target) instanceof org.bukkit.entity.LivingEntity living)) return true;
        return !fire(new com.destroystokyo.paper.event.entity.SlimeTargetLivingEntityEvent(found, living));
    }

    public static boolean orbMerge(net.minecraft.world.entity.Entity first, net.minecraft.world.entity.Entity second) {
        if (!listened(com.destroystokyo.paper.event.entity.ExperienceOrbMergeEvent.getHandlerList()) || !mobReady(first)
                || !(bukkit(first) instanceof org.bukkit.entity.ExperienceOrb a) || !(bukkit(second) instanceof org.bukkit.entity.ExperienceOrb b)) return true;
        return !fire(new com.destroystokyo.paper.event.entity.ExperienceOrbMergeEvent(a, b));
    }

    public static boolean enterBlock(net.minecraft.world.entity.Entity entity, Level level, BlockPos pos) {
        if (level == null || !listened(org.bukkit.event.entity.EntityEnterBlockEvent.getHandlerList()) || !ready(level) || bukkit(entity) == null) return true;
        return !fire(new org.bukkit.event.entity.EntityEnterBlockEvent(bukkit(entity), CraftBlock.at((ServerLevel) level, pos)));
    }

    public static boolean fireworkExplode(net.minecraft.world.entity.Entity firework) {
        if (!listened(org.bukkit.event.entity.FireworkExplodeEvent.getHandlerList()) || !mobReady(firework)
                || !(bukkit(firework) instanceof org.bukkit.entity.Firework found)) return true;
        return !fire(new org.bukkit.event.entity.FireworkExplodeEvent(found));
    }

    public static boolean lingering(net.minecraft.world.entity.Entity potion, net.minecraft.world.entity.Entity cloud) {
        if (!listened(org.bukkit.event.entity.LingeringPotionSplashEvent.getHandlerList()) || !mobReady(potion)
                || !(bukkit(potion) instanceof org.bukkit.entity.ThrownPotion thrown) || !(bukkit(cloud) instanceof org.bukkit.entity.AreaEffectCloud area)) return true;
        return !fire(new org.bukkit.event.entity.LingeringPotionSplashEvent(thrown, area));
    }

    public static boolean spawnerSpawn(net.minecraft.world.entity.Entity entity, Level level, BlockPos pos) {
        if (!listened(org.bukkit.event.entity.SpawnerSpawnEvent.getHandlerList()) || !ready(level) || bukkit(entity) == null
                || !(CraftBlock.at((ServerLevel) level, pos).getState() instanceof org.bukkit.block.CreatureSpawner spawner)) return true;
        return !fire(new org.bukkit.event.entity.SpawnerSpawnEvent(bukkit(entity), spawner));
    }

    public static boolean inside(net.minecraft.world.entity.Entity entity, Level level, BlockPos pos) {
        if (!listened(io.papermc.paper.event.entity.EntityInsideBlockEvent.getHandlerList()) || !ready(level) || bukkit(entity) == null) return true;
        return !fire(new io.papermc.paper.event.entity.EntityInsideBlockEvent(bukkit(entity), CraftBlock.at((ServerLevel) level, pos)));
    }

    public static void move(LivingEntity entity) {
        if (entity instanceof net.minecraft.server.level.ServerPlayer || !listened(io.papermc.paper.event.entity.EntityMoveEvent.getHandlerList())
                || !mobReady(entity) || !(bukkit(entity) instanceof org.bukkit.entity.LivingEntity living)) return;
        double dx = entity.getX() - entity.xo;
        double dy = entity.getY() - entity.yo;
        double dz = entity.getZ() - entity.zo;
        if (dx * dx + dy * dy + dz * dz < 1.0E-8D) return;
        org.bukkit.World world = world(entity.level());
        org.bukkit.Location from = new org.bukkit.Location(world, entity.xo, entity.yo, entity.zo, entity.yRotO, entity.xRotO);
        org.bukkit.Location to = new org.bukkit.Location(world, entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot());
        if (fire(new io.papermc.paper.event.entity.EntityMoveEvent(living, from, to))) entity.setPos(entity.xo, entity.yo, entity.zo);
    }

    public static boolean gameEvent(net.minecraft.core.Holder<net.minecraft.world.level.gameevent.GameEvent> holder,
                                    net.minecraft.world.phys.Vec3 position, net.minecraft.world.level.gameevent.GameEvent.Context context,
                                    ServerLevel level) {
        if (!listened(org.bukkit.event.world.GenericGameEvent.getHandlerList())) return true;
        net.minecraft.resources.ResourceLocation key = net.minecraft.core.registries.BuiltInRegistries.GAME_EVENT.getKey(holder.value());
        org.bukkit.GameEvent gameEvent = key == null ? null : org.bukkit.Registry.GAME_EVENT.get(org.bukkit.craftbukkit.util.CraftNamespacedKey.fromMinecraft(key));
        org.bukkit.World world = world(level);
        if (gameEvent == null || world == null) return true;
        org.bukkit.Location location = new org.bukkit.Location(world, position.x, position.y, position.z);
        return !fire(new org.bukkit.event.world.GenericGameEvent(gameEvent, location, bukkit(context.sourceEntity()),
                holder.value().notificationRadius(), !org.bukkit.Bukkit.isPrimaryThread()));
    }

    public static boolean whitelistUpdate(com.mojang.authlib.GameProfile profile, boolean added) {
        if (!listened(io.papermc.paper.event.server.WhitelistStateUpdateEvent.getHandlerList())) return true;
        return !fire(new io.papermc.paper.event.server.WhitelistStateUpdateEvent(
                new com.destroystokyo.paper.profile.CraftPlayerProfile(profile.getId(), profile.getName()),
                added ? io.papermc.paper.event.server.WhitelistStateUpdateEvent.WhitelistStatus.ADDED
                        : io.papermc.paper.event.server.WhitelistStateUpdateEvent.WhitelistStatus.REMOVED));
    }

    public static void breakProgress(ServerLevel level, int entityId, BlockPos pos, int progress) {
        if (!listened(io.papermc.paper.event.block.BlockBreakProgressUpdateEvent.getHandlerList()) || !org.bukkit.Bukkit.isPrimaryThread()) return;
        net.minecraft.world.entity.Entity entity = level.getEntity(entityId);
        if (entity == null || bukkit(entity) == null) return;
        fire(new io.papermc.paper.event.block.BlockBreakProgressUpdateEvent(CraftBlock.at(level, pos),
                net.minecraft.util.Mth.clamp(progress, 0, 10) / 10.0F, bukkit(entity)));
    }

    public static int expCooldown(net.minecraft.world.entity.player.Player who, int cooldown) {
        org.bukkit.entity.Player player = player(who);
        if (player == null || !listened(org.bukkit.event.player.PlayerExpCooldownChangeEvent.getHandlerList())) return cooldown;
        org.bukkit.event.player.PlayerExpCooldownChangeEvent event = new org.bukkit.event.player.PlayerExpCooldownChangeEvent(player, cooldown,
                org.bukkit.event.player.PlayerExpCooldownChangeEvent.ChangeReason.PICKUP_ORB);
        fire(event);
        return event.getNewCooldown();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void asyncCommands(net.minecraft.server.level.ServerPlayer who, com.mojang.brigadier.tree.RootCommandNode root) {
        org.bukkit.entity.Player player = player(who);
        if (player == null || !listened(com.destroystokyo.paper.event.brigadier.AsyncPlayerSendCommandsEvent.getHandlerList())) return;
        fire(new com.destroystokyo.paper.event.brigadier.AsyncPlayerSendCommandsEvent(player, root, false));
    }

    public static boolean pickItem(net.minecraft.server.level.ServerPlayer who, int sourceSlot) {
        org.bukkit.entity.Player player = player(who);
        if (player == null || !listened(io.papermc.paper.event.player.PlayerPickItemEvent.getHandlerList())) return true;
        return !fire(new io.papermc.paper.event.player.PlayerPickItemEvent(player, who.getInventory().getSuitableHotbarSlot(), sourceSlot));
    }

    public static boolean toggleSit(net.minecraft.world.entity.Entity entity, boolean sitting) {
        if (!listened(io.papermc.paper.event.entity.EntityToggleSitEvent.getHandlerList()) || !mobReady(entity) || bukkit(entity) == null) return true;
        return !fire(new io.papermc.paper.event.entity.EntityToggleSitEvent(bukkit(entity), sitting));
    }

    public static boolean arrowCount(LivingEntity entity, int oldCount, int newCount) {
        if (oldCount == newCount || !listened(org.bukkit.event.entity.ArrowBodyCountChangeEvent.getHandlerList()) || !mobReady(entity)
                || !(bukkit(entity) instanceof org.bukkit.entity.LivingEntity living)) return true;
        return !fire(new org.bukkit.event.entity.ArrowBodyCountChangeEvent(living, oldCount, newCount, newCount == 0));
    }

    public static boolean blockDestroy(Level level, BlockPos pos, boolean drop) {
        if (!listened(com.destroystokyo.paper.event.block.BlockDestroyEvent.getHandlerList()) || !ready(level)) return true;
        ServerLevel serverLevel = (ServerLevel) level;
        net.minecraft.world.level.block.state.BlockState state = serverLevel.getBlockState(pos);
        if (state.isAir()) return true;
        net.minecraft.world.level.block.state.BlockState replacement = serverLevel.getFluidState(pos).createLegacyBlock();
        return !fire(new com.destroystokyo.paper.event.block.BlockDestroyEvent(CraftBlock.at(serverLevel, pos),
                org.bukkit.craftbukkit.block.data.CraftBlockData.fromData(state), org.bukkit.craftbukkit.block.data.CraftBlockData.fromData(replacement), 3, drop));
    }

    public static boolean endermanLooked(net.minecraft.world.entity.Entity enderman, net.minecraft.world.entity.player.Player who) {
        org.bukkit.entity.Player player = player(who);
        if (player == null || !listened(com.destroystokyo.paper.event.entity.EndermanAttackPlayerEvent.getHandlerList()) || !mobReady(enderman)
                || !(bukkit(enderman) instanceof org.bukkit.entity.Enderman found)) return true;
        return !fire(new com.destroystokyo.paper.event.entity.EndermanAttackPlayerEvent(found, player));
    }

    public static boolean pufferState(net.minecraft.world.entity.Entity fish, int state) {
        if (!listened(io.papermc.paper.event.entity.PufferFishStateChangeEvent.getHandlerList()) || !mobReady(fish)
                || !(bukkit(fish) instanceof org.bukkit.entity.PufferFish found)) return true;
        return !fire(new io.papermc.paper.event.entity.PufferFishStateChangeEvent(found, state));
    }

    public static boolean dragonPhase(net.minecraft.world.entity.boss.enderdragon.EnderDragon dragon, int from, int to) {
        if (from == to || !listened(org.bukkit.event.entity.EnderDragonChangePhaseEvent.getHandlerList()) || !mobReady(dragon)
                || !(bukkit(dragon) instanceof org.bukkit.entity.EnderDragon found)) return true;
        org.bukkit.entity.EnderDragon.Phase[] phases = org.bukkit.entity.EnderDragon.Phase.values();
        if (from < 0 || from >= phases.length || to < 0 || to >= phases.length) return true;
        return !fire(new org.bukkit.event.entity.EnderDragonChangePhaseEvent(found, phases[from], phases[to]));
    }

    public static int piglinAnger(net.minecraft.world.entity.monster.ZombifiedPiglin piglin, int anger) {
        if (!listened(org.bukkit.event.entity.PigZombieAngerEvent.getHandlerList()) || !(piglin.level() instanceof ServerLevel level)
                || !org.bukkit.Bukkit.isPrimaryThread() || !(bukkit(piglin) instanceof org.bukkit.entity.PigZombie found)) return anger;
        java.util.UUID targetId = piglin.getPersistentAngerTarget();
        net.minecraft.world.entity.Entity target = targetId == null ? null : level.getEntity(targetId);
        org.bukkit.event.entity.PigZombieAngerEvent event = new org.bukkit.event.entity.PigZombieAngerEvent(found, bukkit(target), anger);
        return fire(event) ? -1 : event.getNewAnger();
    }

    public static boolean deepSleep(net.minecraft.world.entity.player.Player who) {
        org.bukkit.entity.Player player = player(who);
        if (player == null || !listened(io.papermc.paper.event.player.PlayerDeepSleepEvent.getHandlerList())) return true;
        return !fire(new io.papermc.paper.event.player.PlayerDeepSleepEvent(player));
    }

    public static boolean turtleGoHome(net.minecraft.world.entity.Entity turtle) {
        if (!listened(com.destroystokyo.paper.event.entity.TurtleGoHomeEvent.getHandlerList()) || !mobReady(turtle)
                || !(bukkit(turtle) instanceof org.bukkit.entity.Turtle found)) return true;
        return !fire(new com.destroystokyo.paper.event.entity.TurtleGoHomeEvent(found));
    }

    private static final java.util.Map<net.minecraft.world.Container, net.minecraft.world.level.block.entity.LecternBlockEntity> LECTERNS =
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    public static void registerLectern(net.minecraft.world.Container access, net.minecraft.world.level.block.entity.LecternBlockEntity lectern) {
        LECTERNS.put(access, lectern);
    }

    private static org.bukkit.block.Lectern lectern(net.minecraft.world.Container access) {
        net.minecraft.world.level.block.entity.LecternBlockEntity entity = LECTERNS.get(access);
        if (entity == null || !(entity.getLevel() instanceof ServerLevel level)) return null;
        return CraftBlock.at(level, entity.getBlockPos()).getState() instanceof org.bukkit.block.Lectern found ? found : null;
    }

    public static boolean lecternTake(net.minecraft.world.entity.player.Player who, net.minecraft.world.Container access) {
        org.bukkit.entity.Player player = player(who);
        if (player == null || !listened(org.bukkit.event.player.PlayerTakeLecternBookEvent.getHandlerList())) return true;
        org.bukkit.block.Lectern lectern = lectern(access);
        return lectern == null || !fire(new org.bukkit.event.player.PlayerTakeLecternBookEvent(player, lectern));
    }

    public static boolean lecternPage(net.minecraft.world.entity.player.Player who, net.minecraft.world.Container access, int oldPage, int newPage) {
        org.bukkit.entity.Player player = player(who);
        if (player == null || oldPage == newPage || !listened(io.papermc.paper.event.player.PlayerLecternPageChangeEvent.getHandlerList())) return true;
        org.bukkit.block.Lectern lectern = lectern(access);
        if (lectern == null) return true;
        return !fire(new io.papermc.paper.event.player.PlayerLecternPageChangeEvent(player, lectern,
                CraftItemStack.asBukkitCopy(access.getItem(0)),
                newPage > oldPage ? io.papermc.paper.event.player.PlayerLecternPageChangeEvent.PageChangeDirection.RIGHT
                        : io.papermc.paper.event.player.PlayerLecternPageChangeEvent.PageChangeDirection.LEFT, oldPage, newPage));
    }

    public static void entitiesLoaded(ServerLevel level, net.minecraft.world.level.ChunkPos pos, List<net.minecraft.world.entity.Entity> entities) {
        if (!listened(org.bukkit.event.world.EntitiesLoadEvent.getHandlerList()) || !org.bukkit.Bukkit.isPrimaryThread() || entities.isEmpty()) return;
        org.bukkit.craftbukkit.CraftWorld world = LunarArcServerAccess.getCraftWorld(level);
        if (world == null) return;
        List<org.bukkit.entity.Entity> list = new ArrayList<>();
        for (net.minecraft.world.entity.Entity entity : entities) if (bukkit(entity) != null) list.add(bukkit(entity));
        fire(new org.bukkit.event.world.EntitiesLoadEvent(new org.bukkit.craftbukkit.CraftChunk(world, pos.x, pos.z), java.util.Collections.unmodifiableList(list)));
    }

    public static void entitiesUnloaded(ServerLevel level, net.minecraft.world.level.ChunkPos pos, List<net.minecraft.world.entity.Entity> entities) {
        if (!listened(org.bukkit.event.world.EntitiesUnloadEvent.getHandlerList()) || !org.bukkit.Bukkit.isPrimaryThread() || entities.isEmpty()) return;
        org.bukkit.craftbukkit.CraftWorld world = LunarArcServerAccess.getCraftWorld(level);
        if (world == null) return;
        List<org.bukkit.entity.Entity> list = new ArrayList<>();
        for (net.minecraft.world.entity.Entity entity : entities) if (bukkit(entity) != null) list.add(bukkit(entity));
        fire(new org.bukkit.event.world.EntitiesUnloadEvent(new org.bukkit.craftbukkit.CraftChunk(world, pos.x, pos.z), java.util.Collections.unmodifiableList(list)));
    }

    public static void bedFailed(net.minecraft.server.level.ServerPlayer who, BlockPos pos, String problem, net.minecraft.network.chat.Component message) {
        org.bukkit.entity.Player player = player(who);
        if (player == null || !listened(io.papermc.paper.event.player.PlayerBedFailEnterEvent.getHandlerList())) return;
        io.papermc.paper.event.player.PlayerBedFailEnterEvent.FailReason reason;
        try {
            reason = io.papermc.paper.event.player.PlayerBedFailEnterEvent.FailReason.valueOf(problem);
        } catch (IllegalArgumentException unknown) {
            reason = io.papermc.paper.event.player.PlayerBedFailEnterEvent.FailReason.OTHER_PROBLEM;
        }
        boolean explode = reason == io.papermc.paper.event.player.PlayerBedFailEnterEvent.FailReason.NOT_POSSIBLE_HERE;
        fire(new io.papermc.paper.event.player.PlayerBedFailEnterEvent(player, reason, CraftBlock.at(who.serverLevel(), pos), explode,
                message == null ? null : io.papermc.paper.adventure.PaperAdventure.asAdventure(message)));
    }
}
