package io.lunararcdevs.lunararc.neoforge.mixin.core.server;

import io.lunararcdevs.lunararc.common.bridge.MinecraftServerBridge;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.TicketStorage;
import net.neoforged.neoforge.common.world.chunk.ForcedChunkManager;
import org.spongepowered.asm.mixin.Shadow;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin_NeoForge implements MinecraftServerBridge {
    @Override
    public void lunararc$loaderLevelLoad(ServerLevel level) {
        NeoForge.EVENT_BUS.post(new LevelEvent.Load(level));
    }

    @Override
    public void lunararc$loaderLevelUnload(ServerLevel level) {
        NeoForge.EVENT_BUS.post(new LevelEvent.Unload(level));
    }

    @Override
    public void lunararc$loaderMarkLevelsDirty() {
        // NeoForge 26.x dropped MinecraftServer#markWorldsDirty; flag the per-level chunk tickets so they are saved again.
        for (ServerLevel level : ((net.minecraft.server.MinecraftServer) (Object) this).getAllLevels()) {
            TicketStorage tickets = level.getDataStorage().get(TicketStorage.TYPE);
            if (tickets != null) tickets.setDirty();
        }
    }

    @Override
    public void lunararc$loaderReinstatePersistentChunks(ServerLevel level) {
        TicketStorage tickets = level.getDataStorage().get(TicketStorage.TYPE);
        if (tickets == null) return;
        ForcedChunkManager.activateAllDeactivatedTickets(level, tickets);
        tickets.activateAllDeactivatedTickets();
    }

    @Override
    public boolean lunararc$loaderHandlesBlockBreakEvent() {
        return true;
    }

    @Override
    public boolean lunararc$loaderHandlesBlockPlaceEvent() {
        return true;
    }

    @Override
    public boolean lunararc$loaderHandlesEntityJoinEvent() {
        return true;
    }
}
