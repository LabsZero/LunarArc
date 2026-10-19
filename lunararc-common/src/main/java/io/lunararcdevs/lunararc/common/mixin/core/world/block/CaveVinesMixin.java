package io.lunararcdevs.lunararc.common.mixin.core.world.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.lunararcdevs.lunararc.common.compat.LunarArcHarvestCapture;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CaveVines.class)
public interface CaveVinesMixin {
    @WrapOperation(method = "use", require = 1, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;dropFromBlockInteractLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/item/ItemInstance;Lnet/minecraft/world/entity/Entity;Ljava/util/function/BiConsumer;)Z"))
    private static boolean lunararc$harvest(ServerLevel level, ResourceKey<LootTable> table, BlockPos lootPos, BlockState state, BlockEntity blockEntity, net.minecraft.world.item.ItemInstance tool,
            Entity harvester, BiConsumer<ServerLevel, ItemStack> consumer, Operation<Boolean> original, Entity entity, BlockState unusedState, Level unusedLevel, BlockPos pos) {
        if (entity != null && !CraftEventFactory.callEntityChangeBlockEvent(entity, pos, state.setValue(CaveVines.BERRIES, false))) {
            LunarArcHarvestCapture.cancel();
            return false;
        }
        BiConsumer<ServerLevel, ItemStack> captured = (lvl, drop) ->
                LunarArcHarvestCapture.harvest(lvl, pos, entity instanceof Player player ? player : null, drop, item -> consumer.accept(lvl, item));
        return original.call(level, table, lootPos, state, blockEntity, tool, harvester, captured);
    }

    @Inject(method = "use", cancellable = true, require = 1, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;playSound(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/BlockPos;Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V"))
    private static void lunararc$harvestCancelled(Entity entity, BlockState state, Level level, BlockPos pos, CallbackInfoReturnable<InteractionResult> cir) {
        if (LunarArcHarvestCapture.consumeCancelled()) cir.setReturnValue(InteractionResult.SUCCESS);
    }
}
