package io.lunararcdevs.lunararc.common.mixin.core.entity;

import net.minecraft.world.entity.npc.WanderingTrader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.Predicate;

@Mixin(WanderingTrader.class)
public abstract class WanderingTraderMixin {
    private static final String USE_ITEM_GOAL = "Lnet/minecraft/world/entity/ai/goal/UseItemGoal;<init>("
            + "Lnet/minecraft/world/entity/Mob;Lnet/minecraft/world/item/ItemStack;"
            + "Lnet/minecraft/sounds/SoundEvent;Ljava/util/function/Predicate;)V";

    public boolean canDrinkPotion = true;
    public boolean canDrinkMilk = true;

    @ModifyArg(method = "registerGoals", index = 3, at = @At(value = "INVOKE", ordinal = 0, target = USE_ITEM_GOAL))
    private Predicate<WanderingTrader> lunararc$potionPredicate(Predicate<WanderingTrader> original) {
        return trader -> this.canDrinkPotion && original.test(trader);
    }

    @ModifyArg(method = "registerGoals", index = 3, at = @At(value = "INVOKE", ordinal = 1, target = USE_ITEM_GOAL))
    private Predicate<WanderingTrader> lunararc$milkPredicate(Predicate<WanderingTrader> original) {
        return trader -> this.canDrinkMilk && original.test(trader);
    }
}
