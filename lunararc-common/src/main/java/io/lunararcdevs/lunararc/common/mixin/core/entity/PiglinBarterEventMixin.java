package io.lunararcdevs.lunararc.common.mixin.core.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.lunararcdevs.lunararc.common.event.LunarArcMoreEvents;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(PiglinAi.class)
public abstract class PiglinBarterEventMixin {

    @WrapOperation(method = "stopHoldingOffHandItem", require = 0,
            at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/world/entity/monster/piglin/PiglinAi;throwItems(Lnet/minecraft/world/entity/monster/piglin/Piglin;Ljava/util/List;)V"))
    private static void lunararc$barter(Piglin piglin, List<ItemStack> items, Operation<Void> original) {
        List<ItemStack> outcome = LunarArcMoreEvents.piglinBarter(piglin, items);
        if (outcome != null) original.call(piglin, outcome);
    }
}
