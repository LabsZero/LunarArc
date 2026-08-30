package io.ampznetwork.lunararc.common.bridge;

import net.minecraft.world.entity.Entity;
import org.bukkit.event.entity.CreatureSpawnEvent;

public interface ServerLevelBridge {
    boolean lunararc$addFreshEntity(Entity entity, CreatureSpawnEvent.SpawnReason reason);
    void lunararc$addFreshEntityWithPassengers(Entity entity, CreatureSpawnEvent.SpawnReason reason);
}
