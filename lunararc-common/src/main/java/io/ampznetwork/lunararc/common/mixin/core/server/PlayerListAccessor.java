package io.ampznetwork.lunararc.common.mixin.core.server;

import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.ServerOpList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import net.minecraft.server.level.ServerPlayer;

@Mixin(PlayerList.class)
public interface PlayerListAccessor {
    @Accessor("ops")
    ServerOpList getOps();

    /** Exposes PlayerList#save(ServerPlayer) to the Bukkit Player#saveData bridge. */
    @Invoker("save")
    void lunararc$save(ServerPlayer player);
}
