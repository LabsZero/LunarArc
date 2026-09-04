package io.ampznetwork.lunararc.common.bridge;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * CraftBukkit's {@code useItemOn}/{@code handleUseItem} interact-event dedup state,
 * implemented by {@code ServerPlayerGameModeMixin}.
 *
 * <p>Real CraftBukkit's {@code handleUseItem} packet handler raytraces for a block in
 * range and, if one is found, reuses the {@link org.bukkit.event.player.PlayerInteractEvent}
 * result {@code useItemOn} already produced for that same block/hand/item instead of firing
 * a second, wrongly-classified {@code RIGHT_CLICK_AIR} event for what is really one physical
 * click - the client sends both {@code ServerboundUseItemOnPacket} and (when the block use did
 * not consume the interaction) {@code ServerboundUseItemPacket} for it. This bridge is what
 * lets the two mixins - one per packet - share that state.</p>
 */
public interface ServerPlayerGameModeBridge {
    boolean lunararc$firedInteract();

    boolean lunararc$interactResult();

    BlockPos lunararc$interactPosition();

    InteractionHand lunararc$interactHand();

    ItemStack lunararc$interactItemStack();

    void lunararc$clearFiredInteract();
}
