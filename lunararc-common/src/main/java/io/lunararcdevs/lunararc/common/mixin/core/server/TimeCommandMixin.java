package io.lunararcdevs.lunararc.common.mixin.core.server;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.lunararcdevs.lunararc.common.event.LunarArcPaperEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.server.commands.TimeCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.clock.ServerClockManager;
import net.minecraft.world.clock.WorldClock;
import org.bukkit.event.world.TimeSkipEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Fires {@link TimeSkipEvent} for {@code /time set} and {@code /time add}, which now drive a {@link WorldClock}. */
@Mixin(TimeCommand.class)
public abstract class TimeCommandMixin {
    @WrapOperation(method = "setTotalTicks", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/clock/ServerClockManager;setTotalTicks(Lnet/minecraft/core/Holder;J)V"))
    private static void lunararc$timeSet(ServerClockManager manager, Holder<WorldClock> clock, long ticks, Operation<Void> original,
                                         @Local(argsOnly = true) CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        long current = manager.getInstance(clock).totalTicks();
        long skip = LunarArcPaperEvents.timeSkip(level, TimeSkipEvent.SkipReason.COMMAND, ticks - current);
        if (skip != 0L) original.call(manager, clock, current + skip);
    }

    @WrapOperation(method = "addTime", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/clock/ServerClockManager;addTicks(Lnet/minecraft/core/Holder;I)V"))
    private static void lunararc$timeAdd(ServerClockManager manager, Holder<WorldClock> clock, int ticks, Operation<Void> original,
                                         @Local(argsOnly = true) CommandSourceStack source) {
        long skip = LunarArcPaperEvents.timeSkip(source.getLevel(), TimeSkipEvent.SkipReason.COMMAND, ticks);
        if (skip != 0L) original.call(manager, clock, (int) skip);
    }
}
