package io.lunararcdevs.lunararc.common.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftItemStack;

import java.util.List;
import java.util.function.Consumer;

public final class LunarArcHarvestCapture {
    private static boolean cancelled;

    private LunarArcHarvestCapture() {}

    public static void harvest(Level level, BlockPos pos, Player player, ItemStack drop, Consumer<ItemStack> popper) {
        if (!(level instanceof ServerLevel) || player == null) {
            popper.accept(drop);
            return;
        }
        var event = CraftEventFactory.callPlayerHarvestBlockEvent(level, pos, player, InteractionHand.MAIN_HAND, List.of(drop));
        if (event.isCancelled()) {
            cancelled = true;
            return;
        }
        for (org.bukkit.inventory.ItemStack item : event.getItemsHarvested()) popper.accept(CraftItemStack.asNMSCopy(item));
    }

    public static void cancel() {
        cancelled = true;
    }

    public static boolean consumeCancelled() {
        boolean was = cancelled;
        cancelled = false;
        return was;
    }
}
