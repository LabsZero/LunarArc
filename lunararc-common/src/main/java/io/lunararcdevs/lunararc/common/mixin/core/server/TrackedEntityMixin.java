package io.lunararcdevs.lunararc.common.mixin.core.server;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.lunararcdevs.lunararc.common.event.LunarArcPaperEvents;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(net.minecraft.server.level.ChunkMap.TrackedEntity.class)
public abstract class TrackedEntityMixin {

    @Shadow @Final net.minecraft.world.entity.Entity entity;

    @WrapOperation(method = "updatePlayer", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerEntity;addPairing(Lnet/minecraft/server/level/ServerPlayer;)V"))
    private void lunararc$track(ServerEntity serverEntity, ServerPlayer player, Operation<Void> original) {
        if (!LunarArcPaperEvents.trackEntity(player, this.entity)) original.call(serverEntity, player);
    }

    @WrapOperation(method = "updatePlayer", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerEntity;removePairing(Lnet/minecraft/server/level/ServerPlayer;)V"))
    private void lunararc$untrack(ServerEntity serverEntity, ServerPlayer player, Operation<Void> original) {
        original.call(serverEntity, player);
        LunarArcPaperEvents.untrackEntity(player, this.entity);
    }
}
