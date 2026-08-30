package io.ampznetwork.lunararc.common.bridge;

import net.minecraft.server.level.ServerPlayer;
import javax.annotation.Nullable;

public interface AbstractContainerMenuBridge {
    @Nullable ServerPlayer lunararc$getOwner();
    void lunararc$setOwner(@Nullable ServerPlayer owner);
    boolean lunararc$getCheckReachable();
    void lunararc$setCheckReachable(boolean checkReachable);
}
