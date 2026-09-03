package io.ampznetwork.lunararc.common.mixin.core.world;

import io.ampznetwork.lunararc.common.mod.server.LunarArcTickingTrackerImpl;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Block-entity tick tracking.
 *
 * <p>This used to live in {@code ServerLevelTickingMixin} as an @Inject into
 * {@code ServerLevel#tickBlockEntities()V}, which never applied: {@code tickBlockEntities} is
 * declared on {@code Level}, not {@code ServerLevel}, so Mixin could not find it and (being
 * {@code require = 0}) silently did nothing. Retargeting alone would not have fixed it either -
 * {@code Level#tickBlockEntities} only iterates {@code blockEntityTickers} calling
 * {@link net.minecraft.world.level.block.entity.TickingBlockEntity#tick()}, so neither the
 * {@code BlockEntityTicker#tick} invoke the old hook aimed at nor any {@code BlockEntity} local
 * exists in that method.</p>
 *
 * <p>The ticker call actually happens one level down, in {@code LevelChunk$BoundTickingBlockEntity
 * #tick()}, where the block entity is a field rather than a local - so that is what this hooks,
 * shadowing the field instead of capturing a local.</p>
 *
 * <p>Injected at HEAD/RETURN rather than around the ticker invoke so the push and pop are always
 * balanced: {@code tick()} returns early when the position is not ticking or the block state no
 * longer matches, which with an invoke-site push would pop without ever having pushed. The tracker
 * is a single-slot ThreadLocal, so covering the whole method costs nothing.</p>
 */
// remap = false to match this project's other inner-class string targets (Slime$SlimeFloatGoal and
// friends): the runtime is Mojang-mapped, so the target name and members are already correct.
@Mixin(targets = "net.minecraft.world.level.chunk.LevelChunk$BoundTickingBlockEntity", remap = false)
public abstract class BoundTickingBlockEntityMixin {

    @Shadow @Final private BlockEntity blockEntity;

    @Inject(method = "tick", at = @At("HEAD"), require = 0)
    private void lunararc$pushTickingBlockEntity(CallbackInfo ci) {
        LunarArcTickingTrackerImpl.pushBlockEntity(this.blockEntity);
    }

    @Inject(method = "tick", at = @At("RETURN"), require = 0)
    private void lunararc$popTickingBlockEntity(CallbackInfo ci) {
        LunarArcTickingTrackerImpl.pop();
    }
}
