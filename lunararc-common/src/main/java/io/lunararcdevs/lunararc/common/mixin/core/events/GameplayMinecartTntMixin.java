package io.lunararcdevs.lunararc.common.mixin.core.events;

import io.lunararcdevs.lunararc.common.event.LunarArcGameplayEvents;
import net.minecraft.world.entity.vehicle.minecart.MinecartTNT;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** EntityIgniteEvent for a TNT minecart whose fuse is lit: plugins may change the fuse or cancel it (a negative fuse leaves it unlit). */
@Mixin(MinecartTNT.class)
public abstract class GameplayMinecartTntMixin {
    @ModifyConstant(method = "primeFuse", constant = @Constant(intValue = 80), require = 1)
    private int lunararc$ignite(int fuse) {
        return LunarArcGameplayEvents.ignite((MinecartTNT) (Object) this, fuse);
    }
}
