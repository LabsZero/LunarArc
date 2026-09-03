package io.ampznetwork.lunararc.common.bridge;

import net.minecraft.world.entity.Entity;
import org.bukkit.event.entity.CreatureSpawnEvent;

public interface ServerLevelBridge {
    boolean lunararc$addFreshEntity(Entity entity, CreatureSpawnEvent.SpawnReason reason);
    void lunararc$addFreshEntityWithPassengers(Entity entity, CreatureSpawnEvent.SpawnReason reason);

    /**
     * Where this dimension's data actually lives on disk, or null if it could not be established.
     *
     * <p>Vanilla keeps no reference to the {@code LevelStorageAccess} a level was built from, so
     * this is captured from the constructor argument, which is what Arclight does for the same
     * reason. It answers correctly for both kinds of world: for the dimensions the server creates
     * it is the vanilla folder under the main save ({@code world/DIM-1} and friends, since all of
     * them share the one access), and for a world a plugin created through
     * CraftServer#createWorld it is that world's own directory.</p>
     */
    java.nio.file.Path lunararc$getDimensionFolder();
}
