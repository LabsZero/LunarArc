package io.lunararcdevs.lunararc.common.mixin.core.network;

import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerboundInteractPacket.class)
public interface ServerboundInteractPacketAccessor extends io.lunararcdevs.lunararc.common.bridge.access.ServerboundInteractPacketAccessBridge {
    @Accessor("entityId")
    int lunararc$getEntityId();
}
