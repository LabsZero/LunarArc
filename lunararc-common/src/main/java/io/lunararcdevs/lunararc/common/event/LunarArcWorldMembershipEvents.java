package io.lunararcdevs.lunararc.common.event;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import io.lunararcdevs.lunararc.common.LunarArcServerAccess;
import io.lunararcdevs.lunararc.common.bridge.EntityBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.event.Event;

public final class LunarArcWorldMembershipEvents {
    private LunarArcWorldMembershipEvents() {}

    public static void fireAdded(Entity entity) {
        if (EntityAddToWorldEvent.getHandlerList().getRegisteredListeners().length == 0) return;
        fire(entity, true);
    }

    public static void fireRemoved(Entity entity) {
        if (EntityRemoveFromWorldEvent.getHandlerList().getRegisteredListeners().length == 0) return;
        fire(entity, false);
    }

    private static void fire(Entity entity, boolean added) {
        if (!(entity.level() instanceof ServerLevel level) || !level.getServer().isSameThread()) return;
        org.bukkit.craftbukkit.CraftServer craftServer = LunarArcServerAccess.getCraftServer(level.getServer());
        CraftWorld world = craftServer.getCraftWorldIfPresent(level);
        if (world == null) return;
        if (!(((EntityBridge) entity).lunararc$getBukkitEntity() instanceof org.bukkit.entity.Entity bukkitEntity)) return;
        Event event = added
                ? new EntityAddToWorldEvent(bukkitEntity, world)
                : new EntityRemoveFromWorldEvent(bukkitEntity, world);
        craftServer.getPluginManager().callEvent(event);
    }
}
