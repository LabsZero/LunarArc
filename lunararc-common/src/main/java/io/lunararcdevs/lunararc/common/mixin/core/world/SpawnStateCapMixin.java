package io.lunararcdevs.lunararc.common.mixin.core.world;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.lunararcdevs.lunararc.common.server.LunarArcServerSettings;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.world.level.NaturalSpawner$SpawnState", remap = false)
public abstract class SpawnStateCapMixin {
    @WrapOperation(method = "canSpawnForCategory", remap = true, require = 0,
            at = @At(value = "INVOKE", remap = true,
                    target = "Lnet/minecraft/world/entity/MobCategory;getMaxInstancesPerChunk()I"))
    private int lunararc$categoryCap(MobCategory category, Operation<Integer> original) {
        return LunarArcServerSettings.limit(category, original.call(category));
    }

    @WrapOperation(method = "canSpawnForCategory", remap = true, require = 0,
            at = @At(value = "INVOKE", remap = true,
                    target = "Lit/unimi/dsi/fastutil/objects/Object2IntOpenHashMap;getInt(Ljava/lang/Object;)I"))
    private int lunararc$globalCount(it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap<MobCategory> counts, Object category, Operation<Integer> original) {
        return LunarArcServerSettings.perPlayerMobSpawns() ? 0 : original.call(counts, category);
    }

    @WrapMethod(method = "canSpawnForCategory", remap = true, require = 0)
    private boolean lunararc$categoryCadence(MobCategory category, ChunkPos pos, Operation<Boolean> original) {
        return LunarArcServerSettings.cadenceAllows(category) && original.call(category, pos);
    }
}
