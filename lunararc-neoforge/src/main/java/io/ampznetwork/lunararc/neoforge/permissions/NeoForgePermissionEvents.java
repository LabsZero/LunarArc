package io.ampznetwork.lunararc.neoforge.permissions;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;

// No explicit bus: NeoForge 1.21.1 routes a subscriber by the event's own type, which is why
// EventBusSubscriber.bus()/Bus are deprecated for removal. PermissionGatherEvent is a game-bus
// event and still reaches this handler - NeoForge's own code subscribes to mod-bus and game-bus
// events through this same bare annotation (NetworkInitialization vs MonsterRoomHooks).
@EventBusSubscriber(modid = "lunararc")
public final class NeoForgePermissionEvents {
    private NeoForgePermissionEvents() {}

    @SubscribeEvent
    public static void onPermissionGather(PermissionGatherEvent.Handler event) {
        event.addPermissionHandler(
                ResourceLocation.fromNamespaceAndPath("lunararc", "bukkit"),
                LunarArcPermissionHandler::new);
    }
}
