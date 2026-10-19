package io.lunararcdevs.lunararc.common.event;

import io.lunararcdevs.lunararc.common.LunarArcServerAccess;
import io.lunararcdevs.lunararc.common.bridge.EntityBridge;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.CraftEquipmentSlot;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.CraftNamespacedKey;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Paper/Bukkit events that are not part of the main event classes. Every method checks for listeners
 * first and only runs on the primary thread, so servers with nothing listening pay almost nothing.
 */
public final class LunarArcExtraEvents {
    private LunarArcExtraEvents() {}

    private static boolean listened(HandlerList handlers) {
        return handlers.getRegisteredListeners().length != 0;
    }

    private static boolean fire(Event event) {
        LunarArcServerAccess.getCraftServer().getPluginManager().callEvent(event);
        return event instanceof Cancellable cancellable && cancellable.isCancelled();
    }

    private static Object bukkitObject(Entity entity) {
        return entity == null ? null : ((EntityBridge) entity).lunararc$getBukkitEntity();
    }

    private static org.bukkit.entity.Player player(Entity entity) {
        return entity instanceof ServerPlayer && bukkitObject(entity) instanceof org.bukkit.entity.Player found ? found : null;
    }

    private static boolean serverSide(Entity entity) {
        return entity != null && entity.level() instanceof ServerLevel && org.bukkit.Bukkit.isPrimaryThread();
    }

    private record Change(org.bukkit.inventory.ItemStack oldItem, org.bukkit.inventory.ItemStack newItem)
            implements io.papermc.paper.event.entity.EntityEquipmentChangedEvent.EquipmentChange {}

    /** Equipment of a living entity changed; fires the generic event and, for players, the armor event. */
    public static void equipmentChanged(LivingEntity entity, Map<EquipmentSlot, ItemStack> added, Map<EquipmentSlot, ItemStack> previous) {
        if (added == null || added.isEmpty() || !serverSide(entity)) return;
        if (!(bukkitObject(entity) instanceof org.bukkit.entity.LivingEntity living)) return;

        boolean wantsGeneric = listened(io.papermc.paper.event.entity.EntityEquipmentChangedEvent.getHandlerList());
        org.bukkit.entity.Player player = player(entity);
        boolean wantsArmor = player != null && listened(com.destroystokyo.paper.event.player.PlayerArmorChangeEvent.getHandlerList());
        if (!wantsGeneric && !wantsArmor) return;

        Map<org.bukkit.inventory.EquipmentSlot, io.papermc.paper.event.entity.EntityEquipmentChangedEvent.EquipmentChange> changes =
                new EnumMap<>(org.bukkit.inventory.EquipmentSlot.class);
        for (Map.Entry<EquipmentSlot, ItemStack> entry : added.entrySet()) {
            ItemStack old = previous.getOrDefault(entry.getKey(), ItemStack.EMPTY);
            org.bukkit.inventory.EquipmentSlot slot = CraftEquipmentSlot.getSlot(entry.getKey());
            org.bukkit.inventory.ItemStack oldItem = CraftItemStack.asBukkitCopy(old);
            org.bukkit.inventory.ItemStack newItem = CraftItemStack.asBukkitCopy(entry.getValue());
            changes.put(slot, new Change(oldItem, newItem));

            if (wantsArmor) {
                com.destroystokyo.paper.event.player.PlayerArmorChangeEvent.SlotType type = switch (entry.getKey()) {
                    case HEAD -> com.destroystokyo.paper.event.player.PlayerArmorChangeEvent.SlotType.HEAD;
                    case CHEST -> com.destroystokyo.paper.event.player.PlayerArmorChangeEvent.SlotType.CHEST;
                    case LEGS -> com.destroystokyo.paper.event.player.PlayerArmorChangeEvent.SlotType.LEGS;
                    case FEET -> com.destroystokyo.paper.event.player.PlayerArmorChangeEvent.SlotType.FEET;
                    default -> null;
                };
                if (type != null) fire(new com.destroystokyo.paper.event.player.PlayerArmorChangeEvent(player, type, oldItem, newItem));
            }
        }
        if (wantsGeneric) fire(new io.papermc.paper.event.entity.EntityEquipmentChangedEvent(living, changes));
    }

    /**
     * An item cooldown is being applied to a player. Returns the cooldown to apply in ticks, or -1 if a
     * plugin cancelled it. {@code material} is null when the cooldown was set by group rather than by item.
     */
    public static int cooldown(ServerPlayer who, org.bukkit.Material material, Identifier group, int ticks) {
        if (!serverSide(who)) return ticks;
        org.bukkit.entity.Player player = player(who);
        if (player == null) return ticks;
        org.bukkit.NamespacedKey key = CraftNamespacedKey.fromMinecraft(group);
        if (material != null) {
            if (!listened(io.papermc.paper.event.player.PlayerItemCooldownEvent.getHandlerList())
                    && !listened(io.papermc.paper.event.player.PlayerItemGroupCooldownEvent.getHandlerList())) return ticks;
            io.papermc.paper.event.player.PlayerItemCooldownEvent event =
                    new io.papermc.paper.event.player.PlayerItemCooldownEvent(player, material, key, ticks);
            return fire(event) ? -1 : event.getCooldown();
        }
        if (!listened(io.papermc.paper.event.player.PlayerItemGroupCooldownEvent.getHandlerList())) return ticks;
        io.papermc.paper.event.player.PlayerItemGroupCooldownEvent event =
                new io.papermc.paper.event.player.PlayerItemGroupCooldownEvent(player, key, ticks);
        return fire(event) ? -1 : event.getCooldown();
    }

    public static void difficultyChanged(net.minecraft.commands.CommandSourceStack source, net.minecraft.world.Difficulty difficulty) {
        if (!listened(io.papermc.paper.event.world.WorldDifficultyChangeEvent.getHandlerList()) || !org.bukkit.Bukkit.isPrimaryThread()) return;
        org.bukkit.Difficulty bukkit = org.bukkit.Difficulty.getByValue(difficulty.getId());
        if (bukkit == null) return;
        org.bukkit.World world = LunarArcServerAccess.getCraftWorld(source.getLevel());
        if (world == null) return;
        fire(new io.papermc.paper.event.world.WorldDifficultyChangeEvent(world,
                (io.papermc.paper.command.brigadier.CommandSourceStack) source, bukkit));
    }

    public static void fishHookState(net.minecraft.world.entity.projectile.FishingHook hook,
            net.minecraft.world.entity.projectile.FishingHook.FishHookState before) {
        if (hook.currentState == before || !serverSide(hook)
                || !listened(io.papermc.paper.event.entity.FishHookStateChangeEvent.getHandlerList())) return;
        if (!(bukkitObject(hook) instanceof org.bukkit.entity.FishHook bukkit)) return;
        org.bukkit.entity.FishHook.HookState state = switch (hook.currentState) {
            case FLYING -> org.bukkit.entity.FishHook.HookState.UNHOOKED;
            case HOOKED_IN_ENTITY -> org.bukkit.entity.FishHook.HookState.HOOKED_ENTITY;
            case BOBBING -> org.bukkit.entity.FishHook.HookState.BOBBING;
        };
        fire(new io.papermc.paper.event.entity.FishHookStateChangeEvent(bukkit, state));
    }

    /** A potion effect is about to tick; returns false if a plugin cancelled this tick. */
    public static boolean effectTick(LivingEntity entity, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int amplifier) {
        if (!serverSide(entity) || !listened(io.papermc.paper.event.entity.EntityEffectTickEvent.getHandlerList())) return true;
        if (!(bukkitObject(entity) instanceof org.bukkit.entity.LivingEntity living)) return true;
        org.bukkit.potion.PotionEffectType type = org.bukkit.craftbukkit.potion.CraftPotionEffectType.minecraftHolderToBukkit(effect);
        return type == null || !fire(new io.papermc.paper.event.entity.EntityEffectTickEvent(living, type, amplifier));
    }

    // ---- crafting, maps, menus, borders ----

    public static void itemCrafted(net.minecraft.world.entity.player.Player who, ItemStack result) {
        if (result.isEmpty() || !(who instanceof ServerPlayer) || !org.bukkit.Bukkit.isPrimaryThread()
                || !listened(io.papermc.paper.event.inventory.ItemCraftedEvent.getHandlerList())) return;
        org.bukkit.entity.Player player = player(who);
        if (player != null) fire(new io.papermc.paper.event.inventory.ItemCraftedEvent(player, CraftItemStack.asBukkitCopy(result)));
    }

    /** A player filled an empty map; returns the map stack to hand out (a plugin may replace it). */
    public static ItemStack mapFilled(net.minecraft.world.entity.player.Player who, ItemStack emptyMapBefore, ItemStack created) {
        if (!(who instanceof ServerPlayer) || !org.bukkit.Bukkit.isPrimaryThread()
                || !listened(io.papermc.paper.event.player.PlayerMapFilledEvent.getHandlerList())) return created;
        org.bukkit.entity.Player player = player(who);
        if (player == null) return created;
        io.papermc.paper.event.player.PlayerMapFilledEvent event = new io.papermc.paper.event.player.PlayerMapFilledEvent(
                player, CraftItemStack.asBukkitCopy(emptyMapBefore), CraftItemStack.asCraftMirror(created));
        fire(event);
        return CraftItemStack.unwrap(event.getCreatedMap());
    }

    /** Loom pattern chosen. Returns the pattern to use, or null if a plugin cancelled the selection. */
    public static net.minecraft.core.Holder<net.minecraft.world.level.block.entity.BannerPattern> loomPattern(
            net.minecraft.world.entity.player.Player who, net.minecraft.world.inventory.AbstractContainerMenu menu,
            net.minecraft.core.Holder<net.minecraft.world.level.block.entity.BannerPattern> chosen) {
        if (!(who instanceof ServerPlayer) || !org.bukkit.Bukkit.isPrimaryThread()
                || !listened(io.papermc.paper.event.player.PlayerLoomPatternSelectEvent.getHandlerList())) return chosen;
        org.bukkit.entity.Player player = player(who);
        org.bukkit.inventory.InventoryView view = ((io.lunararcdevs.lunararc.common.bridge.AbstractContainerMenuBridge) menu).lunararc$getBukkitView();
        if (player == null || view == null || !(view.getTopInventory() instanceof org.bukkit.inventory.LoomInventory loom)) return chosen;
        io.papermc.paper.event.player.PlayerLoomPatternSelectEvent event = new io.papermc.paper.event.player.PlayerLoomPatternSelectEvent(
                player, loom, org.bukkit.craftbukkit.block.banner.CraftPatternType.minecraftHolderToBukkit(chosen));
        if (fire(event)) return null;
        return org.bukkit.craftbukkit.block.banner.CraftPatternType.bukkitToMinecraftHolder(event.getPatternType());
    }

    /** Result of the beacon effect event: effects to apply and whether to consume the payment, or null if cancelled. */
    public record BeaconChoice(java.util.Optional<net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>> primary,
                               java.util.Optional<net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>> secondary,
                               boolean consume) {}

    public static BeaconChoice beaconEffects(net.minecraft.world.entity.player.Player who, org.bukkit.block.Block beacon,
            java.util.Optional<net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>> primary,
            java.util.Optional<net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>> secondary) {
        BeaconChoice unchanged = new BeaconChoice(primary, secondary, true);
        if (!(who instanceof ServerPlayer) || beacon == null || !org.bukkit.Bukkit.isPrimaryThread()
                || !listened(io.papermc.paper.event.player.PlayerChangeBeaconEffectEvent.getHandlerList())) return unchanged;
        org.bukkit.entity.Player player = player(who);
        if (player == null) return unchanged;
        io.papermc.paper.event.player.PlayerChangeBeaconEffectEvent event = new io.papermc.paper.event.player.PlayerChangeBeaconEffectEvent(
                player,
                primary.map(org.bukkit.craftbukkit.potion.CraftPotionEffectType::minecraftHolderToBukkit).orElse(null),
                secondary.map(org.bukkit.craftbukkit.potion.CraftPotionEffectType::minecraftHolderToBukkit).orElse(null),
                beacon);
        if (fire(event)) return null;
        return new BeaconChoice(
                java.util.Optional.ofNullable(event.getPrimary() == null ? null : org.bukkit.craftbukkit.potion.CraftPotionEffectType.bukkitToMinecraftHolder(event.getPrimary())),
                java.util.Optional.ofNullable(event.getSecondary() == null ? null : org.bukkit.craftbukkit.potion.CraftPotionEffectType.bukkitToMinecraftHolder(event.getSecondary())),
                event.willConsumeItem());
    }

    public static void borderFinished(net.minecraft.world.level.border.WorldBorder border, double from, double to, double durationTicks) {
        if (!org.bukkit.Bukkit.isPrimaryThread() || !listened(io.papermc.paper.event.world.border.WorldBorderBoundsChangeFinishEvent.getHandlerList())) return;
        net.minecraft.server.MinecraftServer server = LunarArcServerAccess.getMinecraftServer();
        if (server == null) return;
        for (ServerLevel level : server.getAllLevels()) {
            if (level.getWorldBorder() != border) continue;
            org.bukkit.craftbukkit.CraftWorld world = LunarArcServerAccess.getCraftWorld(level);
            if (world != null) fire(new io.papermc.paper.event.world.border.WorldBorderBoundsChangeFinishEvent(world, world.getWorldBorder(), from, to, durationTicks));
            return;
        }
    }

    // ---- warden, login, signs, mace, breeding ----

    /** Warden anger is about to rise; returns the offset to apply, or Integer.MIN_VALUE if a plugin cancelled it. */
    public static int wardenAnger(net.minecraft.world.entity.monster.warden.Warden warden, Entity target, int currentAnger, int offset) {
        if (!serverSide(warden) || target == null || !listened(io.papermc.paper.event.entity.WardenAngerChangeEvent.getHandlerList())) return offset;
        if (!(bukkitObject(warden) instanceof org.bukkit.entity.Warden bukkitWarden) || bukkitObject(target) == null) return offset;
        io.papermc.paper.event.entity.WardenAngerChangeEvent event = new io.papermc.paper.event.entity.WardenAngerChangeEvent(
                bukkitWarden, (org.bukkit.entity.Entity) bukkitObject(target), currentAnger, Math.min(150, currentAnger + offset));
        if (fire(event)) return Integer.MIN_VALUE;
        return event.getNewAnger() - currentAnger;
    }

    /**
     * The server-full check during login. {@code denial} is the vanilla result (the server_full component,
     * another denial, or null). Returns null to allow the login, otherwise the denial to keep.
     */
    public static net.minecraft.network.chat.Component serverFullCheck(net.minecraft.server.players.NameAndId id,
            net.minecraft.network.chat.Component denial, boolean serverIsFull) {
        if (!org.bukkit.Bukkit.isPrimaryThread() || !listened(io.papermc.paper.event.player.PlayerServerFullCheckEvent.getHandlerList())) return denial;
        if (denial != null && !serverIsFull) return denial; // denied for another reason (ban, whitelist)
        net.kyori.adventure.text.Component message = io.lunararcdevs.lunararc.common.messaging.LunarArcComponentPipeline.toAdventure(
                net.minecraft.network.chat.Component.translatable("multiplayer.disconnect.server_full"));
        com.mojang.authlib.GameProfile profile = new com.mojang.authlib.GameProfile(id.id(), id.name());
        io.papermc.paper.event.player.PlayerServerFullCheckEvent event = new io.papermc.paper.event.player.PlayerServerFullCheckEvent(
                com.destroystokyo.paper.profile.CraftPlayerProfile.asBukkitCopy(profile), message, serverIsFull);
        fire(event);
        if (event.isAllowed()) return null;
        return denial != null ? denial : io.lunararcdevs.lunararc.common.messaging.LunarArcComponentPipeline.fromAdventure(message);
    }

    /** A sign's click command is about to run. Returns the command to run, or null if a plugin cancelled it. */
    public static String signCommand(net.minecraft.world.entity.player.Player who, ServerLevel level, net.minecraft.core.BlockPos pos,
            boolean front, String rawCommand) {
        if (!(who instanceof ServerPlayer) || !org.bukkit.Bukkit.isPrimaryThread()
                || !listened(io.papermc.paper.event.player.PlayerSignCommandPreprocessEvent.getHandlerList())) return rawCommand;
        org.bukkit.entity.Player player = player(who);
        if (player == null || !(org.bukkit.craftbukkit.block.CraftBlock.at(level, pos).getState() instanceof org.bukkit.block.Sign sign)) return rawCommand;
        String command = rawCommand.startsWith("/") ? rawCommand : "/" + rawCommand;
        io.papermc.paper.event.player.PlayerSignCommandPreprocessEvent event = new io.papermc.paper.event.player.PlayerSignCommandPreprocessEvent(
                player, command, new java.util.HashSet<>(org.bukkit.Bukkit.getOnlinePlayers()), sign,
                front ? org.bukkit.block.sign.Side.FRONT : org.bukkit.block.sign.Side.BACK);
        if (fire(event)) return null;
        return event.getMessage();
    }

    /** A mace hit is checking whether it may smash; returns the final decision. */
    public static boolean smashAttack(LivingEntity attacker, LivingEntity target, ItemStack weapon, boolean canSmash) {
        if (!serverSide(attacker) || !listened(io.papermc.paper.event.entity.EntityAttemptSmashAttackEvent.getHandlerList())) return canSmash;
        if (!(bukkitObject(attacker) instanceof org.bukkit.entity.LivingEntity a) || !(bukkitObject(target) instanceof org.bukkit.entity.LivingEntity t)) return canSmash;
        io.papermc.paper.event.entity.EntityAttemptSmashAttackEvent event = new io.papermc.paper.event.entity.EntityAttemptSmashAttackEvent(
                a, t, CraftItemStack.asBukkitCopy(weapon), canSmash);
        fire(event);
        org.bukkit.event.Event.Result result = event.getResult();
        return result == org.bukkit.event.Event.Result.ALLOW || (canSmash && result == org.bukkit.event.Event.Result.DEFAULT);
    }

    /** Two animals bred and an egg-laying animal is about to become pregnant; returns false if cancelled. */
    public static boolean fertilizeEgg(net.minecraft.world.entity.animal.Animal mother, net.minecraft.world.entity.animal.Animal father) {
        if (!serverSide(mother) || !listened(io.papermc.paper.event.entity.EntityFertilizeEggEvent.getHandlerList())) return true;
        if (!(bukkitObject(mother) instanceof org.bukkit.entity.LivingEntity m) || !(bukkitObject(father) instanceof org.bukkit.entity.LivingEntity f)) return true;
        ServerPlayer cause = mother.getLoveCause();
        if (cause == null) cause = father.getLoveCause();
        org.bukkit.entity.Player breeder = cause == null ? null : player(cause);
        io.papermc.paper.event.entity.EntityFertilizeEggEvent event = new io.papermc.paper.event.entity.EntityFertilizeEggEvent(
                m, f, breeder, null, mother.getRandom().nextInt(7) + 1);
        if (fire(event)) {
            mother.resetLove();
            father.resetLove();
            return false;
        }
        return true;
    }

    // ---- statistics, stonecutter, compost, sculk, spin attack ----

    /** A player statistic is about to increase; returns true if a plugin cancelled it. */
    public static boolean statisticIncrease(net.minecraft.world.entity.player.Player who, net.minecraft.stats.Stat<?> stat, int current, int newValue) {
        if (!(who instanceof ServerPlayer) || !org.bukkit.Bukkit.isPrimaryThread()
                || !listened(org.bukkit.event.player.PlayerStatisticIncrementEvent.getHandlerList())) return false;
        org.bukkit.entity.Player player = player(who);
        if (player == null) return false;
        org.bukkit.Statistic bukkit = org.bukkit.craftbukkit.CraftStatistic.getBukkitStatistic(stat);
        if (bukkit == null) return false;
        switch (bukkit) {
            case FALL_ONE_CM, BOAT_ONE_CM, CLIMB_ONE_CM, WALK_ON_WATER_ONE_CM, WALK_UNDER_WATER_ONE_CM, FLY_ONE_CM, HORSE_ONE_CM,
                 MINECART_ONE_CM, PIG_ONE_CM, PLAY_ONE_MINUTE, SWIM_ONE_CM, WALK_ONE_CM, SPRINT_ONE_CM, CROUCH_ONE_CM,
                 TIME_SINCE_DEATH, SNEAK_TIME, TOTAL_WORLD_TIME, TIME_SINCE_REST, AVIATE_ONE_CM, STRIDER_ONE_CM,
                 HAPPY_GHAST_ONE_CM, NAUTILUS_ONE_CM -> {
                return false; // far too frequent to report, as in Paper
            }
            default -> { }
        }
        org.bukkit.event.player.PlayerStatisticIncrementEvent event;
        if (bukkit.getType() == org.bukkit.Statistic.Type.UNTYPED) {
            event = new org.bukkit.event.player.PlayerStatisticIncrementEvent(player, bukkit, current, newValue);
        } else if (bukkit.getType() == org.bukkit.Statistic.Type.ENTITY) {
            @SuppressWarnings("unchecked")
            org.bukkit.entity.EntityType type = org.bukkit.craftbukkit.CraftStatistic.getEntityTypeFromStatistic(
                    (net.minecraft.stats.Stat<net.minecraft.world.entity.EntityType<?>>) stat);
            event = new org.bukkit.event.player.PlayerStatisticIncrementEvent(player, bukkit, current, newValue, type);
        } else {
            event = new org.bukkit.event.player.PlayerStatisticIncrementEvent(player, bukkit, current, newValue,
                    org.bukkit.craftbukkit.CraftStatistic.getMaterialFromStatistic(stat));
        }
        return fire(event);
    }

    /**
     * A stonecutter recipe was chosen. Returns the recipe index to use, -2 if a plugin cancelled it, or the
     * original index when nothing changed.
     */
    public static int stonecutterRecipe(net.minecraft.world.entity.player.Player who, net.minecraft.world.inventory.AbstractContainerMenu menu,
            int index, net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.StonecutterRecipe> holder,
            java.util.List<java.util.Optional<net.minecraft.world.item.crafting.RecipeHolder<net.minecraft.world.item.crafting.StonecutterRecipe>>> all) {
        if (!(who instanceof ServerPlayer) || holder == null || !org.bukkit.Bukkit.isPrimaryThread()
                || !listened(io.papermc.paper.event.player.PlayerStonecutterRecipeSelectEvent.getHandlerList())) return index;
        org.bukkit.entity.Player player = player(who);
        org.bukkit.inventory.InventoryView view = ((io.lunararcdevs.lunararc.common.bridge.AbstractContainerMenuBridge) menu).lunararc$getBukkitView();
        if (player == null || view == null || !(view.getTopInventory() instanceof org.bukkit.inventory.StonecutterInventory inventory)) return index;
        if (!(org.bukkit.craftbukkit.inventory.CraftRecipeAdapter.toBukkit(holder) instanceof org.bukkit.inventory.StonecuttingRecipe recipe)) return index;
        io.papermc.paper.event.player.PlayerStonecutterRecipeSelectEvent event =
                new io.papermc.paper.event.player.PlayerStonecutterRecipeSelectEvent(player, inventory, recipe);
        if (fire(event)) return -2;
        org.bukkit.NamespacedKey chosen = event.getStonecuttingRecipe().getKey();
        if (chosen.equals(recipe.getKey())) return index;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).isPresent()
                    && org.bukkit.craftbukkit.inventory.CraftRecipeAdapter.toBukkit(all.get(i).get().id().identifier()).equals(chosen)) return i;
        }
        return index;
    }

    public static boolean compostListened(boolean entityInvolved) {
        return listened(io.papermc.paper.event.block.CompostItemEvent.getHandlerList())
                || (entityInvolved && listened(io.papermc.paper.event.entity.EntityCompostItemEvent.getHandlerList()));
    }

    /** Composter fill: returns the final "raise the level" decision, or null if a plugin cancelled it. */
    public static Boolean compost(Entity entity, net.minecraft.world.level.LevelAccessor level, net.minecraft.core.BlockPos pos,
            ItemStack stack, boolean willRaise) {
        boolean entityEvent = entity != null;
        if (!(level instanceof ServerLevel serverLevel) || !org.bukkit.Bukkit.isPrimaryThread()) return willRaise;
        if (!listened(io.papermc.paper.event.block.CompostItemEvent.getHandlerList())
                && !(entityEvent && listened(io.papermc.paper.event.entity.EntityCompostItemEvent.getHandlerList()))) return willRaise;
        org.bukkit.block.Block block = org.bukkit.craftbukkit.block.CraftBlock.at(serverLevel, pos);
        org.bukkit.inventory.ItemStack item = CraftItemStack.asBukkitCopy(stack);
        io.papermc.paper.event.block.CompostItemEvent event;
        if (!entityEvent) {
            event = new io.papermc.paper.event.block.CompostItemEvent(block, item, willRaise ? 1 : 0);
        } else if (bukkitObject(entity) instanceof org.bukkit.entity.Entity bukkitEntity) {
            event = new io.papermc.paper.event.entity.EntityCompostItemEvent(bukkitEntity, block, item, willRaise ? 1 : 0);
        } else {
            return willRaise;
        }
        if (fire(event)) return null;
        return event.willRaiseLevel();
    }

    /** Sculk bloom: returns the cursor charge to use, or -1 if a plugin cancelled it. */
    public static int sculkBloom(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, int charge) {
        if (!(level instanceof ServerLevel serverLevel) || !org.bukkit.Bukkit.isPrimaryThread()
                || !listened(org.bukkit.event.block.SculkBloomEvent.getHandlerList())) return charge;
        org.bukkit.event.block.SculkBloomEvent event = new org.bukkit.event.block.SculkBloomEvent(
                org.bukkit.craftbukkit.block.CraftBlock.at(serverLevel, pos), charge);
        return fire(event) ? -1 : event.getCharge();
    }

    /** A riptide spin hit a living entity; returns false if a plugin cancelled the attack on it. */
    public static boolean spinAttack(LivingEntity attacker, LivingEntity target) {
        if (!serverSide(attacker) || !listened(io.papermc.paper.event.entity.EntityAttemptSpinAttackEvent.getHandlerList())) return true;
        if (!(bukkitObject(attacker) instanceof org.bukkit.entity.LivingEntity a) || !(bukkitObject(target) instanceof org.bukkit.entity.LivingEntity t)) return true;
        return !fire(new io.papermc.paper.event.entity.EntityAttemptSpinAttackEvent(a, t));
    }
    // ---- merchants and equipment ----

    /** A player trades with a plugin-made merchant; returns true if a plugin cancelled the purchase. */
    public static boolean purchaseCancelled(ServerPlayer who, org.bukkit.inventory.Merchant merchant, net.minecraft.world.item.trading.MerchantOffer offer) {
        if (!org.bukkit.Bukkit.isPrimaryThread() || !listened(io.papermc.paper.event.player.PlayerPurchaseEvent.getHandlerList())) return false;
        org.bukkit.entity.Player player = player(who);
        if (player == null || merchant == null) return false;
        return fire(new io.papermc.paper.event.player.PlayerPurchaseEvent(
                player, merchant, new org.bukkit.craftbukkit.inventory.CraftMerchantRecipe(offer), false, true));
    }

    /** A player right-clicks armor to swap it with the worn piece; returns true if a plugin cancelled the swap. */
    public static boolean equipSwapCancelled(Player who, ItemStack held, EquipmentSlot slot, ItemStack worn) {
        if (!(who instanceof ServerPlayer) || !org.bukkit.Bukkit.isPrimaryThread()
                || !listened(io.papermc.paper.event.player.PlayerSwapWithEquipmentSlotEvent.getHandlerList())) return false;
        org.bukkit.entity.Player player = player(who);
        if (player == null) return false;
        io.papermc.paper.event.player.PlayerSwapWithEquipmentSlotEvent event = new io.papermc.paper.event.player.PlayerSwapWithEquipmentSlotEvent(
                player, CraftItemStack.asBukkitCopy(held), CraftEquipmentSlot.getSlot(slot), CraftItemStack.asBukkitCopy(worn));
        fire(event);
        if (event.isCancelled()) {
            player.updateInventory();
            return true;
        }
        return false;
    }
}
