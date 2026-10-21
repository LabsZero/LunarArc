package io.lunararcdevs.lunararc.common.mixin.core.events;

import io.lunararcdevs.lunararc.common.LunarArcServerAccess;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.bukkit.event.world.AsyncStructureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** AsyncStructureSpawnEvent: a plugin may stop a structure being placed in a chunk; the start is dropped and generation moves on. */
@Mixin(ChunkGenerator.class)
public abstract class GameplayStructureSpawnMixin {
    @Inject(method = "tryGenerateStructure", cancellable = true, require = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/StructureManager;setStartForStructure(Lnet/minecraft/world/level/levelgen/structure/Structure;Lnet/minecraft/world/level/levelgen/structure/StructureStart;Lnet/minecraft/world/level/chunk/StructureAccess;)V"))
    private void lunararc$structureSpawn(net.minecraft.world.level.levelgen.structure.StructureSet.StructureSelectionEntry entry,
            StructureManager manager, net.minecraft.core.RegistryAccess registries, net.minecraft.world.level.levelgen.RandomState randomState,
            net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager templates, long seed,
            net.minecraft.world.level.chunk.ChunkAccess chunk, ChunkPos sourceChunk,
            net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, net.minecraft.world.level.biome.Climate.Sampler sampler,
            CallbackInfoReturnable<Boolean> cir, @com.llamalad7.mixinextras.sugar.Local StructureStart start) {
        if (AsyncStructureSpawnEvent.getHandlerList().getRegisteredListeners().length == 0 || !(manager.level instanceof net.minecraft.world.level.ServerLevelAccessor accessor)) return;
        Structure structure = entry.structure().value();
        BoundingBox box = start.getBoundingBox();
        AsyncStructureSpawnEvent event = new AsyncStructureSpawnEvent(LunarArcServerAccess.getCraftWorld(accessor.getLevel()),
                org.bukkit.craftbukkit.generator.structure.CraftStructure.minecraftToBukkit(structure),
                new org.bukkit.util.BoundingBox(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()),
                sourceChunk.x(), sourceChunk.z());
        org.bukkit.Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) cir.setReturnValue(true);
    }
}
