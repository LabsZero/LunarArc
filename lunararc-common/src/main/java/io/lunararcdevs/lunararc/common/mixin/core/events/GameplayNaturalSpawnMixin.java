package io.lunararcdevs.lunararc.common.mixin.core.events;

import io.lunararcdevs.lunararc.common.event.LunarArcGameplayEvents;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** PlayerNaturallySpawnCreaturesEvent: plugins can shrink a player's natural spawn radius, or stop creatures spawning around them. */
@Mixin(ChunkMap.class)
public abstract class GameplayNaturalSpawnMixin {
    @Shadow
    private boolean playerIsCloseEnoughTo(ServerPlayer player, Vec3 pos, int blocks) {
        throw new AssertionError();
    }

    @Inject(method = "playerIsCloseEnoughForSpawning", at = @At("HEAD"), cancellable = true, require = 1)
    private void lunararc$spawnRadius(ServerPlayer player, ChunkPos chunk, CallbackInfoReturnable<Boolean> cir) {
        int chunks = LunarArcGameplayEvents.naturalSpawnRadius(player);
        if (chunks == LunarArcGameplayEvents.NO_SPAWN_EVENT) return;
        if (chunks < 0 || player.isSpectator()) {
            cir.setReturnValue(false);
            return;
        }
        cir.setReturnValue(this.playerIsCloseEnoughTo(player, Vec3.atCenterOf(chunk.getMiddleBlockPosition(0)), chunks << 4));
    }
}
