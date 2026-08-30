package io.ampznetwork.lunararc.common.mixin.core.world;

import io.ampznetwork.lunararc.common.bridge.access.BlockEntityTypeAccessBridge;
import java.util.Set;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Implements LunarArc's narrow bridge without globally widening vanilla NMS access. */
@Mixin(BlockEntityType.class)
public interface BlockEntityTypeAccessor extends BlockEntityTypeAccessBridge {
    @Override
    @Accessor("validBlocks")
    Set<Block> lunararc$getValidBlocks();
}
