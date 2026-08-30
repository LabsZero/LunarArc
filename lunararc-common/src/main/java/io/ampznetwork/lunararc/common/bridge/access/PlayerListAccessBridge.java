package io.ampznetwork.lunararc.common.bridge.access;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Normal runtime bridge implemented by the corresponding Mixin accessor/invoker. */
public interface PlayerListAccessBridge {
    void lunararc$invokeSave(ServerPlayer player);
}
