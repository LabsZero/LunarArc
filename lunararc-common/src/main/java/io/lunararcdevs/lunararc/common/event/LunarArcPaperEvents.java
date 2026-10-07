package io.lunararcdevs.lunararc.common.event;

import com.destroystokyo.paper.event.entity.EntityJumpEvent;
import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import com.destroystokyo.paper.event.player.PlayerPickupExperienceEvent;
import io.lunararcdevs.lunararc.common.LunarArcServerAccess;
import io.lunararcdevs.lunararc.common.bridge.EntityBridge;
import io.papermc.paper.event.player.PlayerStopUsingItemEvent;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import org.bukkit.craftbukkit.block.CraftBlock;
import net.minecraft.world.entity.item.ItemEntity;
import com.destroystokyo.paper.event.server.WhitelistToggleEvent;
import io.papermc.paper.event.packet.PlayerChunkLoadEvent;
import io.papermc.paper.event.packet.PlayerChunkUnloadEvent;
import io.papermc.paper.event.player.PlayerShieldDisableEvent;
import io.papermc.paper.event.player.PlayerTrackEntityEvent;
import io.papermc.paper.event.player.PlayerUntrackEntityEvent;
import org.bukkit.event.world.SpawnChangeEvent;
import org.bukkit.event.world.TimeSkipEvent;
import org.bukkit.event.entity.EntityAirChangeEvent;
import org.bukkit.event.entity.EntityPoseChangeEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.EntityToggleSwimEvent;
import org.bukkit.event.entity.ExpBottleEvent;
import org.bukkit.event.player.PlayerPickupArrowEvent;
import com.destroystokyo.paper.event.player.PlayerElytraBoostEvent;
import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import org.bukkit.event.block.BrewingStartEvent;
import org.bukkit.event.player.PlayerItemMendEvent;
import com.destroystokyo.paper.event.player.PlayerStartSpectatingEntityEvent;
import com.destroystokyo.paper.event.player.PlayerStopSpectatingEntityEvent;
import io.papermc.paper.event.block.BellRingEvent;
import io.papermc.paper.event.block.TargetHitEvent;
import org.bukkit.event.entity.BatToggleSleepEvent;
import org.bukkit.event.entity.StriderTemperatureChangeEvent;
import com.destroystokyo.paper.ClientOption;
import com.destroystokyo.paper.event.player.PlayerClientOptionsChangeEvent;
import io.papermc.paper.event.player.PlayerTradeEvent;
import io.papermc.paper.event.world.WorldGameRuleChangeEvent;
import org.bukkit.event.block.CampfireStartEvent;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.event.entity.VillagerCareerChangeEvent;
import org.bukkit.event.entity.VillagerReplenishTradeEvent;
import org.bukkit.event.inventory.FurnaceStartSmeltEvent;
import io.papermc.paper.event.world.border.WorldBorderBoundsChangeEvent;
import io.papermc.paper.event.world.border.WorldBorderCenterChangeEvent;
import com.destroystokyo.paper.event.entity.ThrownEggHatchEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.EntityKnockbackByEntityEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.entity.SheepDyeWoolEvent;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockDamageEvent;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.player.PlayerRiptideEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.Vector;

public final class LunarArcPaperEvents {
    private LunarArcPaperEvents() {}

    private static boolean listened(HandlerList handlers) {
        return handlers.getRegisteredListeners().length != 0;
    }

    private static CraftServer server(Entity entity) {
        return entity.level() instanceof ServerLevel level ? LunarArcServerAccess.getCraftServer(level.getServer()) : null;
    }

    private static boolean call(Entity source, Event event) {
        CraftServer server = server(source);
        if (server == null) return false;
        server.getPluginManager().callEvent(event);
        return event instanceof org.bukkit.event.Cancellable cancellable && cancellable.isCancelled();
    }

    private static boolean callGlobal(Event event) {
        LunarArcServerAccess.getCraftServer().getPluginManager().callEvent(event);
        return event instanceof org.bukkit.event.Cancellable cancellable && cancellable.isCancelled();
    }

    public static boolean jump(LivingEntity entity) {
        if (entity.level().isClientSide) return false;
        if (entity instanceof ServerPlayer player) {
            if (!listened(PlayerJumpEvent.getHandlerList())
                    || !(((EntityBridge) player).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Player bukkit)) return false;
            org.bukkit.Location from = bukkit.getLocation();
            return call(player, new PlayerJumpEvent(bukkit, from, from.clone()));
        }
        return listened(EntityJumpEvent.getHandlerList())
                && ((EntityBridge) entity).lunararc$getBukkitEntity() instanceof org.bukkit.entity.LivingEntity bukkit
                && call(entity, new EntityJumpEvent(bukkit));
    }

    public static boolean preAttack(Player player, Entity target) {
        if (!(player instanceof ServerPlayer) || !listened(PrePlayerAttackEntityEvent.getHandlerList())
                || !(((EntityBridge) player).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Player bukkitPlayer)
                || !(((EntityBridge) target).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Entity bukkitTarget)) {
            return false;
        }
        boolean willAttack = target.isAttackable() && !target.skipAttackInteraction(player);
        return call(player, new PrePlayerAttackEntityEvent(bukkitPlayer, bukkitTarget, willAttack));
    }

    public static boolean pickupExperience(Player player, ExperienceOrb orb) {
        if (!(player instanceof ServerPlayer) || player.level().isClientSide || player.takeXpDelay != 0
                || !listened(PlayerPickupExperienceEvent.getHandlerList())
                || !(((EntityBridge) player).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Player bukkitPlayer)
                || !(((EntityBridge) orb).lunararc$getBukkitEntity() instanceof org.bukkit.entity.ExperienceOrb bukkitOrb)) {
            return false;
        }
        return call(player, new PlayerPickupExperienceEvent(bukkitPlayer, bukkitOrb));
    }

    public static void riptide(Player player, ItemStack stack) {
        if (!(player instanceof ServerPlayer) || !listened(PlayerRiptideEvent.getHandlerList())
                || !(((EntityBridge) player).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Player bukkit)) return;
        call(player, new PlayerRiptideEvent(bukkit, CraftItemStack.asCraftMirror(stack)));
    }

    public static void stopUsing(LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player) || !entity.isUsingItem()
                || !listened(PlayerStopUsingItemEvent.getHandlerList())
                || !(((EntityBridge) player).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Player bukkit)) return;
        call(player, new PlayerStopUsingItemEvent(bukkit, CraftItemStack.asCraftMirror(entity.getUseItem()),
                entity.getTicksUsingItem()));
    }

    public static Boolean resurrect(LivingEntity entity, DamageSource source) {
        if (entity.level().isClientSide || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                || !listened(EntityResurrectEvent.getHandlerList())
                || !(((EntityBridge) entity).lunararc$getBukkitEntity() instanceof org.bukkit.entity.LivingEntity bukkit)) {
            return null;
        }
        for (InteractionHand hand : InteractionHand.values()) {
            if (entity.getItemInHand(hand).is(Items.TOTEM_OF_UNDYING)) {
                return call(entity, new EntityResurrectEvent(bukkit,
                        hand == InteractionHand.MAIN_HAND ? EquipmentSlot.HAND : EquipmentSlot.OFF_HAND)) ? Boolean.FALSE : null;
            }
        }
        EntityResurrectEvent event = new EntityResurrectEvent(bukkit);
        event.setCancelled(true);
        call(entity, event);
        if (event.isCancelled()) return null;
        if (entity instanceof ServerPlayer player) player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(Items.TOTEM_OF_UNDYING));
        entity.setHealth(1.0F);
        entity.removeAllEffects();
        entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, 900, 1));
        entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.ABSORPTION, 100, 1));
        entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE, 800, 0));
        entity.level().broadcastEntityEvent(entity, (byte) 35);
        return Boolean.TRUE;
    }

    public static float blockDamage(ServerPlayer player, BlockPos pos, Direction direction, float progress) {
        if (!listened(BlockDamageEvent.getHandlerList())
                || !(((EntityBridge) player).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Player bukkit)) {
            return progress;
        }
        BlockDamageEvent event = new BlockDamageEvent(bukkit, CraftBlock.at(player.serverLevel(), pos),
                CraftBlock.notchToBlockFace(direction), CraftItemStack.asCraftMirror(player.getMainHandItem()), progress >= 1.0F);
        if (call(player, event)) {
            player.connection.send(new ClientboundBlockUpdatePacket(player.serverLevel(), pos));
            return 0.0F;
        }
        return event.getInstaBreak() ? 2.0F : progress;
    }

    public static void blockDamageAbort(ServerPlayer player, BlockPos pos) {
        if (!listened(BlockDamageAbortEvent.getHandlerList())
                || !(((EntityBridge) player).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Player bukkit)) return;
        call(player, new BlockDamageAbortEvent(bukkit, CraftBlock.at(player.serverLevel(), pos),
                CraftItemStack.asCraftMirror(player.getMainHandItem())));
    }

    private record BlockDrops(ServerPlayer player, BlockPos pos, org.bukkit.block.BlockState state, long time,
                              java.util.List<ItemEntity> items) {}

    private static final ThreadLocal<BlockDrops> DROPS = new ThreadLocal<>();

    public static void beginBlockDrops(ServerPlayer player, BlockPos pos) {
        if (!listened(BlockDropItemEvent.getHandlerList())) return;
        DROPS.set(new BlockDrops(player, pos.immutable(), CraftBlock.at(player.serverLevel(), pos).getState(),
                player.level().getGameTime(), new java.util.ArrayList<>()));
    }

    public static boolean captureDrop(Entity entity) {
        BlockDrops drops = DROPS.get();
        if (drops == null || !(entity instanceof ItemEntity item) || entity.level() != drops.player().level()
                || drops.time() != entity.level().getGameTime()) return false;
        drops.items().add(item);
        return true;
    }

    public static void endBlockDrops() {
        BlockDrops drops = DROPS.get();
        if (drops == null) return;
        DROPS.remove();
        if (drops.items().isEmpty()
                || !(((EntityBridge) drops.player()).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Player bukkit)) return;
        ServerLevel level = drops.player().serverLevel();
        java.util.List<org.bukkit.entity.Item> items = new java.util.ArrayList<>(drops.items().size());
        for (ItemEntity item : drops.items()) items.add((org.bukkit.entity.Item) ((EntityBridge) item).lunararc$getBukkitEntity());
        BlockDropItemEvent event = new BlockDropItemEvent(CraftBlock.at(level, drops.pos()), drops.state(), bukkit, items);
        if (call(drops.player(), event)) return;
        for (org.bukkit.entity.Item item : event.getItems()) {
            level.addFreshEntity(((org.bukkit.craftbukkit.entity.CraftEntity) item).getHandle());
        }
    }

    public static int shieldDisable(Player player, LivingEntity attacker) {
        if (!(player instanceof ServerPlayer) || !listened(PlayerShieldDisableEvent.getHandlerList())
                || !(((EntityBridge) player).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Player bukkit)
                || !(((EntityBridge) attacker).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Entity damager)) return 100;
        PlayerShieldDisableEvent event = new PlayerShieldDisableEvent(bukkit, damager, 100);
        return call(player, event) ? -1 : event.getCooldown();
    }

    public static long timeSkip(ServerLevel level, TimeSkipEvent.SkipReason reason, long amount) {
        if (!listened(TimeSkipEvent.getHandlerList())) return amount;
        org.bukkit.World world = LunarArcServerAccess.getCraftWorld(level);
        if (world == null) return amount;
        TimeSkipEvent event = new TimeSkipEvent(world, reason, amount);
        return callGlobal(event) ? 0L : event.getSkipAmount();
    }

    public static void spawnChange(ServerLevel level, BlockPos previous) {
        if (!listened(SpawnChangeEvent.getHandlerList())) return;
        org.bukkit.World world = LunarArcServerAccess.getCraftWorld(level);
        if (world == null) return;
        callGlobal(new SpawnChangeEvent(world, new org.bukkit.Location(world, previous.getX(), previous.getY(), previous.getZ())));
    }

    public static void whitelistToggle(boolean enabled) {
        if (listened(WhitelistToggleEvent.getHandlerList())) callGlobal(new WhitelistToggleEvent(enabled));
    }

    public static void chunkLoad(ServerPlayer player, net.minecraft.world.level.chunk.LevelChunk chunk) {
        if (!listened(PlayerChunkLoadEvent.getHandlerList())
                || !(((EntityBridge) player).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Player bukkit)) return;
        org.bukkit.craftbukkit.CraftWorld world = LunarArcServerAccess.getCraftWorld(player.serverLevel());
        if (world != null) call(player, new PlayerChunkLoadEvent(new org.bukkit.craftbukkit.CraftChunk(chunk, world), bukkit));
    }

    public static void chunkUnload(ServerPlayer player, net.minecraft.world.level.ChunkPos pos) {
        if (!listened(PlayerChunkUnloadEvent.getHandlerList())
                || !(((EntityBridge) player).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Player bukkit)) return;
        org.bukkit.craftbukkit.CraftWorld world = LunarArcServerAccess.getCraftWorld(player.serverLevel());
        if (world != null) call(player, new PlayerChunkUnloadEvent(world.getChunkAt(pos.x, pos.z, false), bukkit));
    }

    public static boolean trackEntity(ServerPlayer player, Entity entity) {
        if (!listened(PlayerTrackEntityEvent.getHandlerList())
                || !(((EntityBridge) player).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Player bukkit)
                || !(((EntityBridge) entity).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Entity tracked)) return false;
        return call(player, new PlayerTrackEntityEvent(bukkit, tracked));
    }

    public static void untrackEntity(ServerPlayer player, Entity entity) {
        if (!listened(PlayerUntrackEntityEvent.getHandlerList())
                || !(((EntityBridge) player).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Player bukkit)
                || !(((EntityBridge) entity).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Entity tracked)) return;
        call(player, new PlayerUntrackEntityEvent(bukkit, tracked));
    }

    private static boolean entityEventsActive(Entity entity) {
        return entity.level() instanceof ServerLevel && org.bukkit.Bukkit.isPrimaryThread();
    }

    public static void poseChange(Entity entity, net.minecraft.world.entity.Pose pose) {
        if (!listened(EntityPoseChangeEvent.getHandlerList()) || pose == entity.getPose() || !entityEventsActive(entity)
                || !(((EntityBridge) entity).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Entity bukkit)) return;
        call(entity, new EntityPoseChangeEvent(bukkit, org.bukkit.entity.Pose.values()[pose.ordinal()]));
    }

    public static int airChange(Entity entity, int amount) {
        if (!listened(EntityAirChangeEvent.getHandlerList()) || amount == entity.getAirSupply() || !entityEventsActive(entity)
                || !(((EntityBridge) entity).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Entity bukkit)) return amount;
        EntityAirChangeEvent event = new EntityAirChangeEvent(bukkit, amount);
        return call(entity, event) ? entity.getAirSupply() : event.getAmount();
    }

    public static boolean toggleSwim(Entity entity, boolean swimming) {
        if (!(entity instanceof LivingEntity) || !listened(EntityToggleSwimEvent.getHandlerList())
                || swimming == entity.isSwimming() || !entityEventsActive(entity)
                || !(((EntityBridge) entity).lunararc$getBukkitEntity() instanceof org.bukkit.entity.LivingEntity bukkit)) return false;
        return call(entity, new EntityToggleSwimEvent(bukkit, swimming));
    }

    public static void removed(Entity entity, Entity.RemovalReason reason) {
        if (!listened(EntityRemoveEvent.getHandlerList()) || entity.isRemoved() || !entityEventsActive(entity)
                || !(((EntityBridge) entity).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Entity bukkit)) return;
        EntityRemoveEvent.Cause cause = switch (reason) {
            case KILLED -> EntityRemoveEvent.Cause.DEATH;
            case DISCARDED -> EntityRemoveEvent.Cause.DISCARD;
            case UNLOADED_TO_CHUNK -> EntityRemoveEvent.Cause.UNLOAD;
            case UNLOADED_WITH_PLAYER -> EntityRemoveEvent.Cause.PLAYER_QUIT;
            case CHANGED_DIMENSION -> null;
        };
        if (cause == null) return;
        call(entity, new EntityRemoveEvent(bukkit, cause));
    }

    public static ExpBottleEvent expBottle(Entity bottle, net.minecraft.world.phys.HitResult hit) {
        if (!listened(ExpBottleEvent.getHandlerList()) || !entityEventsActive(bottle)
                || !(((EntityBridge) bottle).lunararc$getBukkitEntity() instanceof org.bukkit.entity.ThrownExpBottle bukkit)) return null;
        int exp = 3 + bottle.level().random.nextInt(5) + bottle.level().random.nextInt(5);
        org.bukkit.block.Block block = null;
        org.bukkit.block.BlockFace face = null;
        org.bukkit.entity.Entity hitEntity = null;
        if (hit instanceof net.minecraft.world.phys.BlockHitResult blockHit) {
            block = CraftBlock.at((ServerLevel) bottle.level(), blockHit.getBlockPos());
            face = CraftBlock.notchToBlockFace(blockHit.getDirection());
        } else if (hit instanceof net.minecraft.world.phys.EntityHitResult entityHit
                && ((EntityBridge) entityHit.getEntity()).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Entity found) {
            hitEntity = found;
        }
        ExpBottleEvent event = new ExpBottleEvent(bukkit, hitEntity, block, face, exp);
        call(bottle, event);
        return event;
    }

    public static boolean pickupArrow(Player player, net.minecraft.world.entity.projectile.AbstractArrow arrow, ItemStack stack) {
        if (!(player instanceof ServerPlayer) || stack.isEmpty() || !listened(PlayerPickupArrowEvent.getHandlerList())
                || arrow.pickup != net.minecraft.world.entity.projectile.AbstractArrow.Pickup.ALLOWED
                || !(((EntityBridge) player).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Player bukkit)
                || !(((EntityBridge) arrow).lunararc$getBukkitEntity() instanceof org.bukkit.entity.AbstractArrow bukkitArrow)) return false;
        ItemEntity item = new ItemEntity(arrow.level(), arrow.getX(), arrow.getY(), arrow.getZ(), stack);
        if (!(((EntityBridge) item).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Item bukkitItem)) return false;
        return call(player, new PlayerPickupArrowEvent(bukkit, bukkitItem, bukkitArrow));
    }

    public static int redstone(net.minecraft.world.level.Level level, BlockPos pos, int old, int current) {
        if (!listened(org.bukkit.event.block.BlockRedstoneEvent.getHandlerList()) || !(level instanceof ServerLevel serverLevel)
                || !org.bukkit.Bukkit.isPrimaryThread()) return current;
        org.bukkit.event.block.BlockRedstoneEvent event = new org.bukkit.event.block.BlockRedstoneEvent(CraftBlock.at(serverLevel, pos), old, current);
        callGlobal(event);
        return event.getNewCurrent();
    }

    public static boolean redstoneAllows(net.minecraft.world.level.Level level, BlockPos pos, boolean oldPowered, boolean newPowered) {
        if (oldPowered == newPowered) return true;
        return (redstone(level, pos, oldPowered ? 15 : 0, newPowered ? 15 : 0) > 0) == newPowered;
    }

    public static boolean mendListened() {
        return listened(PlayerItemMendEvent.getHandlerList());
    }

    public static int mend(ServerPlayer player, ExperienceOrb orb, ItemStack stack, net.minecraft.world.entity.EquipmentSlot slot,
                           int repair, int experience) {
        if (!(((EntityBridge) player).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Player bukkit)
                || !(((EntityBridge) orb).lunararc$getBukkitEntity() instanceof org.bukkit.entity.ExperienceOrb bukkitOrb)) return repair;
        PlayerItemMendEvent event = new PlayerItemMendEvent(bukkit, CraftItemStack.asCraftMirror(stack),
                org.bukkit.craftbukkit.CraftEquipmentSlot.getSlot(slot), bukkitOrb, repair, repair > 0 ? experience : 0);
        return call(player, event) ? -1 : event.getRepairAmount();
    }

    private static void refund(Player player, ItemStack stack) {
        if (!player.hasInfiniteMaterials()) stack.grow(1);
        if (player instanceof ServerPlayer serverPlayer
                && ((EntityBridge) serverPlayer).lunararc$getBukkitEntity() instanceof org.bukkit.craftbukkit.entity.CraftPlayer craft) {
            craft.updateInventory();
        }
    }

    public static boolean launchProjectile(Entity projectile, ItemStack stack) {
        if (!(projectile instanceof net.minecraft.world.entity.projectile.Projectile launched)
                || !(launched.getOwner() instanceof ServerPlayer player) || !listened(PlayerLaunchProjectileEvent.getHandlerList())
                || !(((EntityBridge) player).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Player bukkit)
                || !(((EntityBridge) projectile).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Projectile bukkitProjectile)) return false;
        PlayerLaunchProjectileEvent event = new PlayerLaunchProjectileEvent(bukkit, CraftItemStack.asCraftMirror(stack), bukkitProjectile);
        if (call(player, event)) {
            refund(player, stack);
            return true;
        }
        if (!event.shouldConsume()) refund(player, stack);
        return false;
    }

    public static boolean elytraBoost(Player player, ItemStack stack, Entity rocket, net.minecraft.world.InteractionHand hand) {
        if (!(player instanceof ServerPlayer) || !listened(PlayerElytraBoostEvent.getHandlerList())
                || !(((EntityBridge) player).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Player bukkit)
                || !(((EntityBridge) rocket).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Firework firework)) return false;
        PlayerElytraBoostEvent event = new PlayerElytraBoostEvent(bukkit, CraftItemStack.asCraftMirror(stack), firework,
                hand == InteractionHand.MAIN_HAND ? EquipmentSlot.HAND : EquipmentSlot.OFF_HAND);
        if (call(player, event)) {
            refund(player, stack);
            return true;
        }
        if (!event.shouldConsume()) refund(player, stack);
        return false;
    }

    public static int brewingStart(net.minecraft.world.level.Level level, BlockPos pos, ItemStack ingredient, int time) {
        if (!listened(BrewingStartEvent.getHandlerList()) || !(level instanceof ServerLevel serverLevel)) return time;
        BrewingStartEvent event = new BrewingStartEvent(CraftBlock.at(serverLevel, pos), CraftItemStack.asCraftMirror(ingredient), time);
        callGlobal(event);
        return event.getBrewingTime();
    }

    public static boolean spectate(ServerPlayer player, Entity newCamera) {
        Entity current = player.getCamera();
        Entity target = newCamera == null ? player : newCamera;
        if (current == target || !(((EntityBridge) player).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Player bukkit)
                || !(((EntityBridge) current).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Entity from)) return false;
        if (target == player) {
            return listened(PlayerStopSpectatingEntityEvent.getHandlerList())
                    && call(player, new PlayerStopSpectatingEntityEvent(bukkit, from));
        }
        return listened(PlayerStartSpectatingEntityEvent.getHandlerList())
                && ((EntityBridge) target).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Entity to
                && call(player, new PlayerStartSpectatingEntityEvent(bukkit, from, to));
    }

    public static boolean batToggle(Entity bat, boolean awake) {
        if (!listened(BatToggleSleepEvent.getHandlerList()) || !entityEventsActive(bat)
                || !(((EntityBridge) bat).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Bat bukkit)) return true;
        return !call(bat, new BatToggleSleepEvent(bukkit, awake));
    }

    public static boolean striderToggle(Entity strider, boolean shivering) {
        if (!listened(StriderTemperatureChangeEvent.getHandlerList()) || !entityEventsActive(strider)
                || !(((EntityBridge) strider).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Strider bukkit)) return true;
        return !call(strider, new StriderTemperatureChangeEvent(bukkit, shivering));
    }

    public static boolean bellRing(net.minecraft.world.level.Level level, BlockPos pos, Direction direction, Entity entity) {
        if (!listened(BellRingEvent.getHandlerList()) || !(level instanceof ServerLevel serverLevel) || !org.bukkit.Bukkit.isPrimaryThread()) return true;
        org.bukkit.entity.Entity bukkit = entity != null
                && ((EntityBridge) entity).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Entity found ? found : null;
        return !callGlobal(new BellRingEvent(CraftBlock.at(serverLevel, pos), CraftBlock.notchToBlockFace(direction), bukkit));
    }

    public static int targetHit(net.minecraft.world.level.LevelAccessor level, Entity entity, net.minecraft.world.phys.BlockHitResult hit, int signal) {
        if (!(entity instanceof net.minecraft.world.entity.projectile.Projectile) || !(level instanceof ServerLevel serverLevel)
                || !listened(TargetHitEvent.getHandlerList()) || !org.bukkit.Bukkit.isPrimaryThread()
                || !(((EntityBridge) entity).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Projectile projectile)) return signal;
        TargetHitEvent event = new TargetHitEvent(projectile, CraftBlock.at(serverLevel, hit.getBlockPos()),
                CraftBlock.notchToBlockFace(hit.getDirection()), signal);
        return callGlobal(event) ? 0 : event.getSignalStrength();
    }

    public static boolean replenish(Entity villager, net.minecraft.world.item.trading.MerchantOffer offer) {
        if (!listened(VillagerReplenishTradeEvent.getHandlerList()) || !entityEventsActive(villager)
                || !(((EntityBridge) villager).lunararc$getBukkitEntity() instanceof org.bukkit.entity.AbstractVillager bukkit)) return true;
        return !call(villager, new VillagerReplenishTradeEvent(bukkit, new org.bukkit.craftbukkit.inventory.CraftMerchantRecipe(offer)));
    }

    public static net.minecraft.world.item.trading.MerchantOffer acquire(Entity villager, net.minecraft.world.item.trading.MerchantOffer offer) {
        if (!listened(VillagerAcquireTradeEvent.getHandlerList()) || !entityEventsActive(villager)
                || !(((EntityBridge) villager).lunararc$getBukkitEntity() instanceof org.bukkit.entity.AbstractVillager bukkit)) return offer;
        VillagerAcquireTradeEvent event = new VillagerAcquireTradeEvent(bukkit, new org.bukkit.craftbukkit.inventory.CraftMerchantRecipe(offer));
        if (call(villager, event)) return null;
        if (event.getRecipe() instanceof org.bukkit.craftbukkit.inventory.CraftMerchantRecipe same && same.getHandle() == offer) return offer;
        org.bukkit.craftbukkit.inventory.CraftMerchantRecipe recipe = org.bukkit.craftbukkit.inventory.CraftMerchantRecipe.fromBukkit(event.getRecipe());
        return recipe.getIngredients().isEmpty() ? null : recipe.toMinecraft();
    }

    public static PlayerTradeEvent trade(Entity villager, Player player, net.minecraft.world.item.trading.MerchantOffer offer) {
        if (!(player instanceof ServerPlayer) || !listened(PlayerTradeEvent.getHandlerList()) || !entityEventsActive(villager)
                || !(((EntityBridge) player).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Player bukkitPlayer)
                || !(((EntityBridge) villager).lunararc$getBukkitEntity() instanceof org.bukkit.entity.AbstractVillager bukkit)) return null;
        PlayerTradeEvent event = new PlayerTradeEvent(bukkitPlayer, bukkit, new org.bukkit.craftbukkit.inventory.CraftMerchantRecipe(offer), true, true);
        call(player, event);
        return event;
    }

    public static boolean career(net.minecraft.world.entity.npc.Villager villager, net.minecraft.world.entity.npc.VillagerData data) {
        net.minecraft.world.entity.npc.VillagerProfession old = villager.getVillagerData().getProfession();
        net.minecraft.world.entity.npc.VillagerProfession target = data.getProfession();
        if (old == target || villager.tickCount == 0 || !listened(VillagerCareerChangeEvent.getHandlerList()) || !entityEventsActive(villager)
                || !(((EntityBridge) villager).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Villager bukkit)) return true;
        org.bukkit.entity.Villager.Profession profession = org.bukkit.Registry.VILLAGER_PROFESSION.get(
                org.bukkit.craftbukkit.util.CraftNamespacedKey.fromMinecraft(
                        net.minecraft.core.registries.BuiltInRegistries.VILLAGER_PROFESSION.getKey(target)));
        if (profession == null) return true;
        VillagerCareerChangeEvent.ChangeReason reason = target == net.minecraft.world.entity.npc.VillagerProfession.NONE
                ? VillagerCareerChangeEvent.ChangeReason.LOSING_JOB : VillagerCareerChangeEvent.ChangeReason.EMPLOYED;
        return !call(villager, new VillagerCareerChangeEvent(bukkit, profession, reason));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static int furnaceStart(net.minecraft.world.level.Level level, BlockPos pos, ItemStack source,
                                   net.minecraft.world.item.crafting.RecipeType recipeType, int totalTime) {
        if (source.isEmpty() || recipeType == null || !listened(FurnaceStartSmeltEvent.getHandlerList())
                || !(level instanceof ServerLevel serverLevel) || !org.bukkit.Bukkit.isPrimaryThread()) return totalTime;
        net.minecraft.world.item.crafting.RecipeHolder<?> recipe = (net.minecraft.world.item.crafting.RecipeHolder<?>) serverLevel.getRecipeManager()
                .getRecipeFor(recipeType, new net.minecraft.world.item.crafting.SingleRecipeInput(source), serverLevel).orElse(null);
        if (recipe == null || !(org.bukkit.craftbukkit.inventory.CraftRecipeAdapter.toBukkit(recipe) instanceof org.bukkit.inventory.CookingRecipe<?> cooking)) return totalTime;
        FurnaceStartSmeltEvent event = new FurnaceStartSmeltEvent(CraftBlock.at(serverLevel, pos),
                CraftItemStack.asCraftMirror(source), cooking, totalTime);
        callGlobal(event);
        return event.getTotalCookTime();
    }

    public static int campfireStart(net.minecraft.world.level.Level level, BlockPos pos, ItemStack food,
                                    net.minecraft.world.item.crafting.RecipeHolder<?> recipe, int cookTime) {
        if (recipe == null || !listened(CampfireStartEvent.getHandlerList()) || !(level instanceof ServerLevel serverLevel)
                || !org.bukkit.Bukkit.isPrimaryThread()
                || !(org.bukkit.craftbukkit.inventory.CraftRecipeAdapter.toBukkit(recipe) instanceof org.bukkit.inventory.CampfireRecipe campfire)) return cookTime;
        CampfireStartEvent event = new CampfireStartEvent(CraftBlock.at(serverLevel, pos), CraftItemStack.asCraftMirror(food), campfire);
        callGlobal(event);
        return event.getTotalCookTime();
    }

    public static boolean gameRule(org.bukkit.World world, org.bukkit.command.CommandSender sender, String name, String value) {
        org.bukkit.GameRule<?> rule = org.bukkit.GameRule.getByName(name);
        if (rule == null || world == null || !listened(WorldGameRuleChangeEvent.getHandlerList())) return true;
        return !callGlobal(new WorldGameRuleChangeEvent(world, sender, rule, value));
    }

    public static void clientOptions(ServerPlayer player, net.minecraft.server.level.ClientInformation info) {
        if (!listened(PlayerClientOptionsChangeEvent.getHandlerList())
                || !(((EntityBridge) player).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Player bukkit)) return;
        callGlobal(new PlayerClientOptionsChangeEvent(bukkit, info.language(), info.viewDistance(),
                ClientOption.ChatVisibility.valueOf(info.chatVisibility().name()), info.chatColors(),
                new com.destroystokyo.paper.PaperSkinParts(info.modelCustomisation()),
                info.mainHand() == net.minecraft.world.entity.HumanoidArm.LEFT ? org.bukkit.inventory.MainHand.LEFT : org.bukkit.inventory.MainHand.RIGHT));
    }

    private static boolean borderReentry;

    private static org.bukkit.craftbukkit.CraftWorld borderWorld(net.minecraft.world.level.border.WorldBorder border) {
        net.minecraft.server.MinecraftServer server = LunarArcServerAccess.getMinecraftServer();
        if (server == null) return null;
        for (ServerLevel level : server.getAllLevels()) {
            if (level.getWorldBorder() == border) return LunarArcServerAccess.getCraftWorld(level);
        }
        return null;
    }

    public static void borderBounds(net.minecraft.world.level.border.WorldBorder border, double size, long duration, boolean moving,
                                    java.util.function.Consumer<double[]> apply) {
        org.bukkit.craftbukkit.CraftWorld world = borderReentry || !listened(WorldBorderBoundsChangeEvent.getHandlerList())
                ? null : borderWorld(border);
        if (world == null) {
            apply.accept(new double[] {border.getSize(), size, duration, 0});
            return;
        }
        double old = border.getSize();
        WorldBorderBoundsChangeEvent.Type type = !moving || old == size
                ? WorldBorderBoundsChangeEvent.Type.INSTANT_MOVE : WorldBorderBoundsChangeEvent.Type.STARTED_MOVE;
        WorldBorderBoundsChangeEvent event = new WorldBorderBoundsChangeEvent(world, world.getWorldBorder(), type, old, size, duration);
        if (callGlobal(event)) return;
        borderReentry = true;
        try {
            apply.accept(new double[] {event.getOldSize(), event.getNewSize(), event.getDuration(),
                    event.getType() == WorldBorderBoundsChangeEvent.Type.STARTED_MOVE && event.getDuration() > 0 ? 1 : 0});
        } finally {
            borderReentry = false;
        }
    }

    public static double[] borderCenter(net.minecraft.world.level.border.WorldBorder border, double x, double z) {
        org.bukkit.craftbukkit.CraftWorld world = borderReentry || !listened(WorldBorderCenterChangeEvent.getHandlerList())
                ? null : borderWorld(border);
        if (world == null) return new double[] {x, z};
        WorldBorderCenterChangeEvent event = new WorldBorderCenterChangeEvent(world, world.getWorldBorder(),
                new org.bukkit.Location(world, border.getCenterX(), 0, border.getCenterZ()), new org.bukkit.Location(world, x, 0, z));
        if (callGlobal(event)) return null;
        return new double[] {event.getNewCenter().getX(), event.getNewCenter().getZ()};
    }

    public static void withBorderReentry(Runnable action) {
        borderReentry = true;
        try {
            action.run();
        } finally {
            borderReentry = false;
        }
    }

    public static boolean entityInteract(Entity entity, net.minecraft.world.level.Level level, BlockPos pos) {
        if (entity instanceof Player || !(level instanceof ServerLevel serverLevel) || !listened(EntityInteractEvent.getHandlerList())
                || !org.bukkit.Bukkit.isPrimaryThread()
                || !(((EntityBridge) entity).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Entity bukkit)) return false;
        return call(entity, new EntityInteractEvent(bukkit, CraftBlock.at(serverLevel, pos)));
    }

    public static boolean placeEntity(Entity entity, Player player, BlockPos clicked, Direction face, InteractionHand hand) {
        if (!(player instanceof ServerPlayer) || !listened(EntityPlaceEvent.getHandlerList()) || !(entity.level() instanceof ServerLevel level)
                || !(((EntityBridge) player).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Player bukkit)
                || !(((EntityBridge) entity).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Entity placed)) return false;
        EntityPlaceEvent event = new EntityPlaceEvent(placed, bukkit, CraftBlock.at(level, clicked), CraftBlock.notchToBlockFace(face),
                hand == InteractionHand.MAIN_HAND ? EquipmentSlot.HAND : EquipmentSlot.OFF_HAND);
        if (call(player, event)) {
            refund(player, player.getItemInHand(hand));
            return true;
        }
        return false;
    }

    public static net.minecraft.world.item.DyeColor sheepDye(Player player, Entity sheep, net.minecraft.world.item.DyeColor color) {
        if (!(player instanceof ServerPlayer) || !listened(SheepDyeWoolEvent.getHandlerList())
                || !(((EntityBridge) player).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Player bukkit)
                || !(((EntityBridge) sheep).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Sheep bukkitSheep)) return color;
        SheepDyeWoolEvent event = new SheepDyeWoolEvent(bukkitSheep, org.bukkit.DyeColor.getByWoolData((byte) color.getId()), bukkit);
        if (call(sheep, event)) return null;
        return net.minecraft.world.item.DyeColor.byId(event.getColor().getWoolData());
    }

    public static int[] eggHatch(Entity egg, boolean hatching, int hatches) {
        if (!entityEventsActive(egg) || !(((EntityBridge) egg).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Egg bukkitEgg)) {
            return new int[] {hatching ? 1 : 0, hatches};
        }
        org.bukkit.entity.EntityType type = org.bukkit.entity.EntityType.CHICKEN;
        if (egg instanceof net.minecraft.world.entity.projectile.Projectile projectile && projectile.getOwner() instanceof ServerPlayer owner
                && listened(PlayerEggThrowEvent.getHandlerList())
                && ((EntityBridge) owner).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Player bukkitOwner) {
            PlayerEggThrowEvent event = new PlayerEggThrowEvent(bukkitOwner, bukkitEgg, hatching, (byte) (hatching ? hatches : 0), type);
            call(owner, event);
            hatching = event.isHatching();
            hatches = hatching ? event.getNumHatches() : 0;
            type = event.getHatchingType();
        }
        if (listened(ThrownEggHatchEvent.getHandlerList())) {
            ThrownEggHatchEvent event = new ThrownEggHatchEvent(bukkitEgg, hatching, (byte) (hatching ? hatches : 0), type);
            call(egg, event);
            hatching = event.isHatching();
            hatches = hatching ? event.getNumHatches() : 0;
        }
        return new int[] {hatching && hatches > 0 ? 1 : 0, hatches};
    }

    public static void knockback(net.minecraft.world.entity.LivingEntity target, Entity attacker, double strength,
                                 net.minecraft.world.phys.Vec3 before) {
        if (!listened(EntityKnockbackByEntityEvent.getHandlerList()) || !entityEventsActive(target)
                || !(((EntityBridge) target).lunararc$getBukkitEntity() instanceof org.bukkit.entity.LivingEntity bukkit)
                || !(((EntityBridge) attacker).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Entity source)) return;
        Vec3 after = target.getDeltaMovement();
        Vector knockback = new Vector(after.x - before.x, after.y - before.y, after.z - before.z);
        EntityKnockbackByEntityEvent event = new EntityKnockbackByEntityEvent(bukkit, source,
                org.bukkit.event.entity.EntityKnockbackEvent.KnockbackCause.ENTITY_ATTACK, strength, knockback.clone(), knockback.clone());
        if (call(target, event)) {
            target.setDeltaMovement(before);
            return;
        }
        if (listened(com.destroystokyo.paper.event.entity.EntityKnockbackByEntityEvent.getHandlerList())) {
            com.destroystokyo.paper.event.entity.EntityKnockbackByEntityEvent paper = new com.destroystokyo.paper.event.entity.EntityKnockbackByEntityEvent(
                    bukkit, source, io.papermc.paper.event.entity.EntityKnockbackEvent.Cause.ENTITY_ATTACK, (float) strength, event.getKnockback().clone());
            if (call(target, paper)) {
                target.setDeltaMovement(before);
                return;
            }
        }
        Vector changed = event.getKnockback();
        target.setDeltaMovement(before.x + changed.getX(), before.y + changed.getY(), before.z + changed.getZ());
    }

    public static Packet<?> velocity(ServerPlayer player, Packet<?> packet) {
        if (!(packet instanceof ClientboundSetEntityMotionPacket) || !listened(PlayerVelocityEvent.getHandlerList())
                || !(((EntityBridge) player).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Player bukkit)) {
            return packet;
        }
        Vec3 motion = player.getDeltaMovement();
        Vector velocity = new Vector(motion.x, motion.y, motion.z);
        PlayerVelocityEvent event = new PlayerVelocityEvent(bukkit, velocity.clone());
        if (call(player, event)) return null;
        if (event.getVelocity().equals(velocity)) return packet;
        Vector changed = event.getVelocity();
        player.setDeltaMovement(changed.getX(), changed.getY(), changed.getZ());
        return new ClientboundSetEntityMotionPacket(player);
    }
}
