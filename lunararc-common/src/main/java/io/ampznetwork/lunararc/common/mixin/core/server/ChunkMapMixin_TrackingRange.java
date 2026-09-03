package io.ampznetwork.lunararc.common.mixin.core.server;

import io.ampznetwork.lunararc.common.server.LunarArcTrackingRange;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Applies Spigot's entity tracking ranges where vanilla picks its own.
 *
 * <p>{@code addEntity} works out a range as {@code clientTrackingRange() * 16} and then builds the
 * TrackedEntity with it. This replaces that number, which is the same place and the same way Spigot
 * does it - so everything downstream, including the clamp against the server view distance, is
 * still vanilla's.</p>
 *
 * <p>The local being modified is the range rather than the update interval, despite the injection
 * point naming {@code updateInterval}. That call is simply the first stable instruction after the
 * range has been computed and vanilla's "is this entity tracked at all" check has passed; index 3
 * is the range local at that point. Arclight injects at exactly the same place.</p>
 */
@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin_TrackingRange {

    @ModifyVariable(
            method = "addEntity",
            index = 3,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType;updateInterval()I"),
            require = 0)
    private int lunararc$trackingRange(int defaultRange, Entity entity) {
        return LunarArcTrackingRange.getEntityTrackingRange(entity, defaultRange);
    }
}
