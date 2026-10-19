package io.lunararcdevs.lunararc.common.mixin.core.events;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.lunararcdevs.lunararc.common.event.LunarArcExtraEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * CompostItemEvent and EntityCompostItemEvent. With no listener vanilla runs untouched. With one, the fill is
 * replayed here so the event sees the real roll; a cancelled fill is reported through a flag so the callers
 * give the item back, which is what Paper does by returning null.
 */
@Mixin(ComposterBlock.class)
public abstract class ExtraCompostEventsMixin {
    @Unique private static boolean lunararc$cancelled;

    @Unique private static ItemStack lunararc$stack;

    @WrapMethod(method = "addLayer", require = 0)
    private static BlockState lunararc$compost(Entity entity, BlockState state, ServerLevel level, BlockPos pos,
            net.minecraft.world.item.component.Compostable compostable, Operation<BlockState> original) {
        ItemStack stack = lunararc$stack;
        if (stack == null || !LunarArcExtraEvents.compostListened(entity != null)) {
            return original.call(entity, state, level, pos, compostable);
        }
        // Vanilla rolls the number of layers from a loot number provider; ask it the same way so the event sees the
        // real outcome (a result of 0 layers means the item only fails to raise the level).
        net.minecraft.world.level.storage.loot.LootParams params = new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_STATE, state)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.atCenterOf(pos))
                .withOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.INTERACTING_ENTITY, entity)
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.BLOCK_INTERACT);
        net.minecraft.world.level.storage.loot.LootContext context =
                new net.minecraft.world.level.storage.loot.LootContext.Builder(params).create(java.util.Optional.empty());
        boolean willRaise = compostable.layers().get(context, 0) > 0;
        Boolean decision = LunarArcExtraEvents.compost(entity, level, pos, stack, willRaise);
        if (decision == null) {
            lunararc$cancelled = true;
            return state;
        }
        if (!decision) return state;
        BlockState result = original.call(entity, state, level, pos, compostable);
        if (result != state && entity != null && !CraftEventFactory.callEntityChangeBlockEvent(entity, pos, result)) {
            level.setBlock(pos, state, 3);
            lunararc$cancelled = true;
            return state;
        }
        return result;
    }

    @WrapMethod(method = "useItemOn", require = 0)
    private InteractionResult lunararc$useItem(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit, Operation<InteractionResult> original) {
        int before = stack.getCount();
        lunararc$cancelled = false;
        InteractionResult result = original.call(stack, state, level, pos, player, hand, hit);
        if (lunararc$cancelled) {
            lunararc$cancelled = false;
            stack.setCount(before);
            return InteractionResult.PASS;
        }
        return result;
    }

    @WrapMethod(method = "insertItem", require = 0)
    private static BlockState lunararc$insert(Entity entity, BlockState state, ServerLevel level, ItemStack stack, BlockPos pos,
            Operation<BlockState> original) {
        int before = stack.getCount();
        lunararc$cancelled = false;
        lunararc$stack = stack;
        BlockState result;
        try {
            result = original.call(entity, state, level, stack, pos);
        } finally {
            lunararc$stack = null;
        }
        if (lunararc$cancelled) {
            lunararc$cancelled = false;
            stack.setCount(before);
            return state;
        }
        return result;
    }
}
