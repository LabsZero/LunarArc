package io.lunararcdevs.lunararc.common.mixin.core.events;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.lunararcdevs.lunararc.common.event.LunarArcGameplayEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.PotentSulfurBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** EntityPotentSulfurLaunchEvent: the geyser ticker's upward push on each entity above an active potent sulfur block. */
@Mixin(PotentSulfurBlockEntity.class)
public abstract class GameplayPotentSulfurMixin {
    // LunarArc: the push lives in a synthetic lambda (the geyser BlockEntityTicker), so the target is its compiled name.
    // lambda$static$5 is the same on vanilla, NeoForge and Forge 26.3; a Minecraft update can renumber it, and
    // require = 1 then fails startup instead of silently dropping the event. Recheck with javap -p -c on PotentSulfurBlockEntity.
    // Locals: BlockPos ordinal 0 is the ticker's own pos (the sulfur block), ordinal 1 is findNoxiousGasSourceBlock's result.
    @WrapOperation(method = "lambda$static$5", require = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;addDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"))
    private static void lunararc$launch(Entity entity, Vec3 push, Operation<Void> original,
            @Local(argsOnly = true) Level level, @Local(ordinal = 0) BlockPos sulfur, @Local(ordinal = 1) BlockPos source) {
        original.call(entity, LunarArcGameplayEvents.sulfurLaunch(entity, level, sulfur, source, push));
    }
}
