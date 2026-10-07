package io.lunararcdevs.lunararc.common.mixin.core.world.block;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.lunararcdevs.lunararc.common.event.LunarArcBlockCapture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TreeGrower.class)
public abstract class TreeGrowerMixin {

    @WrapMethod(method = "growTree", require = 0)
    private boolean lunararc$growTree(ServerLevel level, ChunkGenerator generator, BlockPos pos, BlockState state,
            RandomSource random, Operation<Boolean> original) {
        return LunarArcBlockCapture.growTree(level, pos, () -> original.call(level, generator, pos, state, random));
    }

    @Inject(method = "getConfiguredMegaFeature", at = @At("RETURN"), require = 0)
    private void lunararc$noteMegaFeature(RandomSource random, CallbackInfoReturnable<ResourceKey<ConfiguredFeature<?, ?>>> cir) {
        LunarArcBlockCapture.noteFeature(cir.getReturnValue());
    }

    @Inject(method = "getConfiguredFeature", at = @At("RETURN"), require = 0)
    private void lunararc$noteFeature(RandomSource random, boolean flowers, CallbackInfoReturnable<ResourceKey<ConfiguredFeature<?, ?>>> cir) {
        LunarArcBlockCapture.noteFeature(cir.getReturnValue());
    }
}
