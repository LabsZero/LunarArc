package io.lunararcdevs.lunararc.common.mixin.core.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.monster.Phantom;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.UUID;

@Mixin(Phantom.class)
public abstract class PhantomMixin {
    @Unique private boolean lunararc$shouldBurnInDay = true;
    @Unique private UUID lunararc$spawningEntity;

    public boolean shouldBurnInDay() { return this.lunararc$shouldBurnInDay; }
    public void setShouldBurnInDay(boolean shouldBurnInDay) { this.lunararc$shouldBurnInDay = shouldBurnInDay; }
    public UUID getSpawningEntity() { return this.lunararc$spawningEntity; }
    public void setSpawningEntity(UUID entity) { this.lunararc$spawningEntity = entity; }

    @WrapOperation(method = "aiStep", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/monster/Phantom;isSunBurnTick()Z"))
    private boolean lunararc$burnInDay(Phantom phantom, Operation<Boolean> original) {
        return this.lunararc$shouldBurnInDay && original.call(phantom);
    }
}
