package io.lunararcdevs.lunararc.common.mixin.core.entity;

import io.lunararcdevs.lunararc.common.util.LunarArcShearing;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies the drops a plugin left on PlayerShearEntityEvent instead of vanilla's own roll. */
@Mixin(LivingEntity.class)
public abstract class ShearDropsMixin {
    @Inject(method = "dropFromShearingLootTable", at = @At("HEAD"), cancellable = true)
    private void lunararc$forcedShearDrops(ServerLevel level, ResourceKey<LootTable> table, net.minecraft.world.item.ItemInstance shears,
            BiConsumer<ServerLevel, ItemStack> consumer, CallbackInfo ci) {
        List<ItemStack> forced = LunarArcShearing.take((LivingEntity) (Object) this);
        if (forced == null) return;
        ci.cancel();
        for (ItemStack drop : forced) {
            if (!drop.isEmpty()) consumer.accept(level, drop);
        }
    }
}
