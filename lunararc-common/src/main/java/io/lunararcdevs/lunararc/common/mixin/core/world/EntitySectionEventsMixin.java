package io.lunararcdevs.lunararc.common.mixin.core.world;

import com.llamalad7.mixinextras.sugar.Local;
import io.lunararcdevs.lunararc.common.event.LunarArcMoreEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.entity.ChunkEntities;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(PersistentEntitySectionManager.class)
public abstract class EntitySectionEventsMixin<T extends EntityAccess> {
    @Shadow @Final EntitySectionStorage<T> sectionStorage;

    @Inject(method = "processPendingLoads", require = 0,
            at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/Long2ObjectMap;put(JLjava/lang/Object;)Ljava/lang/Object;"))
    private void lunararc$loaded(CallbackInfo ci, @Local ChunkEntities<T> chunk) {
        List<Entity> entities = new ArrayList<>();
        chunk.getEntities().forEach(access -> {
            if (access instanceof Entity entity) entities.add(entity);
        });
        if (!entities.isEmpty() && entities.get(0).level() instanceof ServerLevel level) {
            LunarArcMoreEvents.entitiesLoaded(level, chunk.getPos(), entities);
        }
    }

    @Inject(method = "processChunkUnload", at = @At("HEAD"), require = 0)
    private void lunararc$unload(long pos, CallbackInfoReturnable<Boolean> cir) {
        List<Entity> entities = new ArrayList<>();
        this.sectionStorage.getExistingSectionsInChunk(pos).flatMap(EntitySection::getEntities).forEach(access -> {
            if (access instanceof Entity entity) entities.add(entity);
        });
        if (!entities.isEmpty() && entities.get(0).level() instanceof ServerLevel level) {
            LunarArcMoreEvents.entitiesUnloaded(level, new ChunkPos(pos), entities);
        }
    }
}
