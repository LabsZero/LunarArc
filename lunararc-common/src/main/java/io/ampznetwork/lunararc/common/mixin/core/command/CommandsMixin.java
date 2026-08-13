package io.ampznetwork.lunararc.common.mixin.core.command;

import com.mojang.brigadier.CommandDispatcher;
import io.ampznetwork.lunararc.common.LunarArcPlatform;
import io.ampznetwork.lunararc.common.server.LunarArcCommandMap;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Commands.class)
public abstract class CommandsMixin {

    @Shadow @Final private CommandDispatcher<CommandSourceStack> dispatcher;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void lunararc$onCommandsInit(Commands.CommandSelection selection,
                                          net.minecraft.commands.CommandBuildContext context,
                                          CallbackInfo ci) {
        LunarArcCommandMap.setDispatcher(this.dispatcher);

        if (LunarArcPlatform.getServer() == null) return;
        org.bukkit.command.CommandMap map = LunarArcPlatform.getServer().getCommandMap();
        if (map instanceof LunarArcCommandMap lunarArcMap) {
            lunarArcMap.syncToBrigadier(this.dispatcher);
        }
    }
}
