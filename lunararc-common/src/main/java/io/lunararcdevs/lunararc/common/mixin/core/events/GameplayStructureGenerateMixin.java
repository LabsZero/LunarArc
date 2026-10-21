package io.lunararcdevs.lunararc.common.mixin.core.events;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.bukkit.craftbukkit.util.CraftStructureTransformer;
import org.bukkit.craftbukkit.util.TransformerLevelAccessor;
import org.bukkit.event.world.AsyncStructureGenerateEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * AsyncStructureGenerateEvent: while a structure is placed into a chunk its pieces write through a transformer level, so plugins can
 * rewrite blocks and entities as they are generated. The event fires for the first piece that actually touches the chunk.
 */
@Mixin(StructureStart.class)
public abstract class GameplayStructureGenerateMixin {
    @Shadow @Final private Structure structure;

    private static final ThreadLocal<Scope> SCOPE = new ThreadLocal<>();

    private static final class Scope {
        final WorldGenLevel level;
        final Structure structure;
        final BoundingBox chunkBox;
        final ChunkPos chunkPos;
        TransformerLevelAccessor accessor;
        CraftStructureTransformer transformer;

        Scope(WorldGenLevel level, Structure structure, BoundingBox chunkBox, ChunkPos chunkPos) {
            this.level = level;
            this.structure = structure;
            this.chunkBox = chunkBox;
            this.chunkPos = chunkPos;
        }

        WorldGenLevel accessor() {
            if (accessor == null) {
                transformer = new CraftStructureTransformer(AsyncStructureGenerateEvent.Cause.WORLD_GENERATION, level, structure, chunkBox, chunkPos);
                accessor = new TransformerLevelAccessor();
                accessor.setDelegate(level);
                accessor.setStructureTransformer(transformer);
            }
            return accessor;
        }

        void discard() {
            if (transformer != null) transformer.discard();
        }
    }

    @WrapMethod(method = "placeInChunk", require = 1)
    private void lunararc$structureScope(WorldGenLevel level, StructureManager manager, ChunkGenerator generator, RandomSource random,
            BoundingBox chunkBox, ChunkPos chunkPos, Operation<Void> original) {
        if (AsyncStructureGenerateEvent.getHandlerList().getRegisteredListeners().length == 0 || SCOPE.get() != null) {
            original.call(level, manager, generator, random, chunkBox, chunkPos);
            return;
        }
        Scope scope = new Scope(level, this.structure, chunkBox, chunkPos);
        SCOPE.set(scope);
        try {
            original.call(level, manager, generator, random, chunkBox, chunkPos);
        } finally {
            SCOPE.remove();
            scope.discard();
        }
    }

    @WrapOperation(method = "placeInChunk", require = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/structure/StructurePiece;postProcess(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/StructureManager;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/util/RandomSource;Lnet/minecraft/world/level/levelgen/structure/BoundingBox;Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/core/BlockPos;)V"))
    private void lunararc$piece(StructurePiece piece, WorldGenLevel level, StructureManager manager, ChunkGenerator generator,
            RandomSource random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos reference, Operation<Void> original) {
        Scope scope = SCOPE.get();
        original.call(piece, scope == null ? level : scope.accessor(), manager, generator, random, chunkBox, chunkPos, reference);
    }
}
