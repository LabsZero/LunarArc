package io.lunararcdevs.lunararc.common.mixin.core.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import org.bukkit.craftbukkit.command.CraftCommandMap;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
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
        this.lunararc$registerMinecraftNamespaceAliases();
        CraftCommandMap.setDispatcher(this.dispatcher);
    }

    @Inject(method = "sendCommands", require = 0,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;send(Lnet/minecraft/network/protocol/Packet;)V"))
    private void lunararc$commandSendEvent(net.minecraft.server.level.ServerPlayer player, CallbackInfo ci,
            @com.llamalad7.mixinextras.sugar.Local com.mojang.brigadier.tree.RootCommandNode<net.minecraft.commands.SharedSuggestionProvider> root) {
        if (org.bukkit.event.player.PlayerCommandSendEvent.getHandlerList().getRegisteredListeners().length == 0
                || !(((io.lunararcdevs.lunararc.common.bridge.EntityBridge) player).lunararc$getBukkitEntity()
                        instanceof org.bukkit.entity.Player bukkitPlayer)) {
            return;
        }
        java.util.Set<String> labels = new java.util.LinkedHashSet<>();
        for (CommandNode<net.minecraft.commands.SharedSuggestionProvider> node : root.getChildren()) labels.add(node.getName());
        org.bukkit.event.player.PlayerCommandSendEvent event =
                new org.bukkit.event.player.PlayerCommandSendEvent(bukkitPlayer, new java.util.LinkedHashSet<>(labels));
        io.lunararcdevs.lunararc.common.LunarArcServerAccess.getCraftServer(player.server).getPluginManager().callEvent(event);
        for (String label : labels) {
            if (!event.getCommands().contains(label)) CraftCommandMap.removeBrigadierChild(root, label);
        }
    }

    @Unique
    private void lunararc$registerMinecraftNamespaceAliases() {
        for (CommandNode<CommandSourceStack> node
                : new java.util.ArrayList<>(this.dispatcher.getRoot().getChildren())) {
            String name = node.getName();
            // Already namespaced, or already aliased: nothing to add.
            if (name.indexOf(':') >= 0) continue;
            String alias = "minecraft:" + name;
            if (this.dispatcher.getRoot().getChild(alias) != null) continue;

            CommandNode<CommandSourceStack> target = node;
            while (target.getRedirect() != null) target = target.getRedirect();

            this.dispatcher.register(
                    LiteralArgumentBuilder.<CommandSourceStack>literal(alias)
                            .executes(target.getCommand())
                            .requires(target.getRequirement())
                            .redirect(target));
        }
    }
}
