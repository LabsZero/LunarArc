package io.lunararcdevs.lunararc.common.mixin.core.events;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.lunararcdevs.lunararc.common.event.LunarArcBlockEvents;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Unique;

/**
 * PlayerShearBlockEvent for beehives. The honeycomb loot is rolled first so the event can show (and change) it;
 * the vanilla honeycomb drop is then replaced with those drops.
 */
@Mixin(BeehiveBlock.class)
public abstract class ExtraBeehiveShearEventMixin {
    @Unique private static List<ItemStack> lunararc$drops;


    @WrapMethod(method = "useItemOn", require = 0)
    private InteractionResult lunararc$shear(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit, Operation<InteractionResult> original) {
        if (!LunarArcBlockEvents.shearListened() || !(level instanceof ServerLevel server) || !stack.is(Items.SHEARS)
                || state.getValue(BeehiveBlock.HONEY_LEVEL) < 5) {
            return original.call(stack, state, level, pos, player, hand, hit);
        }
        List<ItemStack> rolled = new ArrayList<>();
        BlockLootInvoker.lunararc$rollInteractLoot(server, BuiltInLootTables.HARVEST_BEEHIVE, pos, state, level.getBlockEntity(pos), stack, player,
                (l, item) -> rolled.add(item));
        List<ItemStack> result = LunarArcBlockEvents.shearBlock(player, server, pos, stack, hand, rolled);
        if (result == null) return InteractionResult.PASS;
        lunararc$drops = result;
        try {
            return original.call(stack, state, level, pos, player, hand, hit);
        } finally {
            lunararc$drops = null;
        }
    }

    @WrapOperation(method = "useItemOn", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/BeehiveBlock;dropHoneycomb(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/BlockPos;)V"))
    private void lunararc$dropHoneycomb(ServerLevel level, ItemStack stack, BlockState state, BlockEntity blockEntity, Entity entity, BlockPos pos,
            Operation<Void> original) {
        List<ItemStack> drops = lunararc$drops;
        if (drops == null) {
            original.call(level, stack, state, blockEntity, entity, pos);
            return;
        }
        for (ItemStack drop : drops) Block.popResource(level, pos, drop);
    }
}
