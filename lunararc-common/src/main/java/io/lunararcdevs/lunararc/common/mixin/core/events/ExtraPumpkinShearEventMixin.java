package io.lunararcdevs.lunararc.common.mixin.core.events;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.lunararcdevs.lunararc.common.event.LunarArcBlockEvents;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PumpkinBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;

/**
 * PlayerShearBlockEvent for carving a pumpkin. When someone listens, the carve is replayed here so the event
 * sees the seeds before they spawn; a cancelled event leaves the pumpkin untouched.
 */
@Mixin(PumpkinBlock.class)
public abstract class ExtraPumpkinShearEventMixin {
    @WrapMethod(method = "useItemOn", require = 0)
    private InteractionResult lunararc$carve(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit, Operation<InteractionResult> original) {
        if (!LunarArcBlockEvents.shearListened() || !stack.is(Items.SHEARS) || !(level instanceof ServerLevel server)) {
            return original.call(stack, state, level, pos, player, hand, hit);
        }
        Direction face = hit.getDirection();
        Direction facing = face.getAxis() == Direction.Axis.Y ? player.getDirection().getOpposite() : face;
        List<ItemStack> rolled = new ArrayList<>();
        BlockLootInvoker.lunararc$rollInteractLoot(server, BuiltInLootTables.CARVE_PUMPKIN, pos, state, level.getBlockEntity(pos), stack, player,
                (l, item) -> rolled.add(item));
        List<ItemStack> drops = LunarArcBlockEvents.shearBlock(player, server, pos, stack, hand, rolled);
        if (drops == null) return InteractionResult.PASS;

        for (ItemStack drop : drops) {
            ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5 + facing.getStepX() * 0.65, pos.getY() + 0.1,
                    pos.getZ() + 0.5 + facing.getStepZ() * 0.65, drop);
            entity.setDeltaMovement(0.05 * facing.getStepX() + level.getRandom().nextDouble() * 0.02, 0.05,
                    0.05 * facing.getStepZ() + level.getRandom().nextDouble() * 0.02);
            level.addFreshEntity(entity);
        }
        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.PUMPKIN_CARVE, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
        level.setBlock(pos, net.minecraft.world.level.block.Blocks.CARVED_PUMPKIN.defaultBlockState()
                .setValue(net.minecraft.world.level.block.CarvedPumpkinBlock.FACING, facing), 11);
        stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
        level.gameEvent(player, net.minecraft.world.level.gameevent.GameEvent.SHEAR, pos);
        player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(Items.SHEARS));
        return InteractionResult.SUCCESS;
    }
}
