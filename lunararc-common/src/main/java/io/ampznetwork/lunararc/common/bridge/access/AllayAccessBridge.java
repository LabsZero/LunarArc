package io.ampznetwork.lunararc.common.bridge.access;

import net.minecraft.core.BlockPos;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.animal.allay.Allay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Normal runtime bridge implemented by the corresponding Mixin accessor/invoker. */
public interface AllayAccessBridge {
    SimpleContainer lunararc$getInventory();
    BlockPos lunararc$getJukeboxPos();
    void lunararc$setJukeboxPos(BlockPos value);
    long lunararc$getDuplicationCooldown();
    void lunararc$setDuplicationCooldown(long value);
}
