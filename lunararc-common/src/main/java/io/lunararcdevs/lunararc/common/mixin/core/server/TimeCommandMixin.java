package io.lunararcdevs.lunararc.common.mixin.core.server;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.lunararcdevs.lunararc.common.event.LunarArcPaperEvents;
import net.minecraft.server.commands.TimeCommand;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.event.world.TimeSkipEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TimeCommand.class)
public abstract class TimeCommandMixin {

    @WrapOperation(method = {"setTime", "addTime"}, require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setDayTime(J)V"))
    private static void lunararc$timeSkip(ServerLevel level, long time, Operation<Void> original) {
        long skip = LunarArcPaperEvents.timeSkip(level, TimeSkipEvent.SkipReason.COMMAND, time - level.getDayTime());
        if (skip != 0L) original.call(level, level.getDayTime() + skip);
    }
}
