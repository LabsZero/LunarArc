package io.lunararcdevs.lunararc.common.mixin.core.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.lunararcdevs.lunararc.common.event.LunarArcMoreEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Pig.class)
public abstract class MobEvents_PigEventsMixin {
    @WrapOperation(method = "thunderHit", require = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/pig/Pig;convertTo(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/ConversionParams;Lnet/minecraft/world/entity/ConversionParams$AfterConversion;)Lnet/minecraft/world/entity/Mob;"))
    private Mob lunararc$zap(Pig pig, EntityType<?> type, ConversionParams params, ConversionParams.AfterConversion<?> after,
            Operation<Mob> original, @Local(argsOnly = true) ServerLevel level, @Local(argsOnly = true) LightningBolt bolt) {
        // PigZapEvent needs the piglin; build an unspawned stand-in. A null result makes vanilla take its normal lightning path.
        ZombifiedPiglin preview = net.minecraft.world.entity.EntityTypes.ZOMBIFIED_PIGLIN.create(level, EntitySpawnReason.CONVERSION);
        if (preview != null) {
            preview.snapTo(pig.getX(), pig.getY(), pig.getZ(), pig.getYRot(), pig.getXRot());
            if (!LunarArcMoreEvents.pigZap(pig, bolt, preview)) return null;
        }
        return original.call(pig, type, params, after);
    }
}
