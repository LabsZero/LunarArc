package io.ampznetwork.lunararc.common.bridge.access;

import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.PathFinder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Normal runtime bridge implemented by the corresponding Mixin accessor/invoker. */
public interface PathFinderAccessBridge {
    NodeEvaluator lunararc$getNodeEvaluator();
}
