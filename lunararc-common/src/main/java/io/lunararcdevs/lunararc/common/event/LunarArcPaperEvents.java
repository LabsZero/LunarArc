package io.lunararcdevs.lunararc.common.event;

import com.destroystokyo.paper.event.entity.EntityJumpEvent;
import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import com.destroystokyo.paper.event.player.PlayerPickupExperienceEvent;
import io.lunararcdevs.lunararc.common.LunarArcServerAccess;
import io.lunararcdevs.lunararc.common.bridge.EntityBridge;
import io.papermc.paper.event.player.PlayerStopUsingItemEvent;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import net.minecraft.network.protocol.Packet;
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
