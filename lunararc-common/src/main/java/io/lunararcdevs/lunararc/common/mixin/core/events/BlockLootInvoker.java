package io.lunararcdevs.lunararc.common.mixin.core.events;

import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Lets the shear-event mixins roll a block's interaction loot without spawning it. */
@Mixin(Block.class)
public interface BlockLootInvoker {
    @Invoker("dropFromBlockInteractLootTable")
    static boolean lunararc$rollInteractLoot(ServerLevel level, ResourceKey<LootTable> table, BlockPos pos, BlockState state,
            BlockEntity blockEntity, ItemInstance tool, Entity entity, BiConsumer<ServerLevel, ItemStack> consumer) {
        throw new AssertionError();
    }
}
