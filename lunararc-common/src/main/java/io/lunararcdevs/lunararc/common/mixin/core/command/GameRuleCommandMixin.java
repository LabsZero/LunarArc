package io.lunararcdevs.lunararc.common.mixin.core.command;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.brigadier.context.CommandContext;
import io.lunararcdevs.lunararc.common.LunarArcServerAccess;
import io.lunararcdevs.lunararc.common.bridge.CommandSourceStackBridge;
import io.lunararcdevs.lunararc.common.event.LunarArcPaperEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.GameRuleCommand;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameRuleCommand.class)
public abstract class GameRuleCommandMixin {

    @WrapOperation(method = "setRule", require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/GameRules$Value;setFromArgument(Lcom/mojang/brigadier/context/CommandContext;Ljava/lang/String;)V"))
    private static void lunararc$gameRule(GameRules.Value<?> value, CommandContext<CommandSourceStack> context, String name,
            Operation<Void> original, @Local(argsOnly = true) GameRules.Key<?> key) {
        CommandSourceStack source = context.getSource();
        String raw = String.valueOf(context.getArgument(name, Object.class));
        org.bukkit.World world = LunarArcServerAccess.getCraftWorld(source.getLevel());
        org.bukkit.command.CommandSender sender = source instanceof CommandSourceStackBridge bridge ? bridge.lunararc$getBukkitSender() : null;
        if (LunarArcPaperEvents.gameRule(world, sender, key.getId(), raw)) original.call(value, context, name);
    }
}
