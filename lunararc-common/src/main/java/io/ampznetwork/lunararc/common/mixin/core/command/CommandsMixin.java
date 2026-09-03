package io.ampznetwork.lunararc.common.mixin.core.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import io.ampznetwork.lunararc.common.server.LunarArcCommandMap;
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
        LunarArcCommandMap.setDispatcher(this.dispatcher);
    }

    /**
     * The {@code minecraft:} form of every command, which a Bukkit server has and this one did not.
     *
     * <p>{@code /minecraft:gamemode} is not a nicety - it is how you reach the real command when a
     * plugin has taken the plain label, and plugins, scripts and command blocks written for Spigot
     * or Paper use it on purpose for exactly that reason. Vanilla registers no such node, and
     * LunarArc was not adding one, so every one of them was an unknown command.</p>
     *
     * <p>This is Paper's own implementation, in the place Paper puts it - the end of the Commands
     * constructor - including the reason for the loop that flattens redirects: brigadier will not
     * resolve a redirect to a redirect, so aliasing {@code minecraft:tp} straight onto {@code tp}
     * would dead-end, because {@code tp} is itself a redirect to {@code teleport}. Following the
     * chain to the first node that really carries the command is what makes the alias work.</p>
     *
     * <p>One difference worth stating: on this runtime the constructor has already fired the
     * loader's command-registration event by the time it returns, so mod commands are in the tree
     * too and get a {@code minecraft:} alias along with the vanilla ones. Paper, with no mods to
     * consider, namespaces only vanilla. The extra aliases are harmless - they resolve to the same
     * node - and the alternative is hooking a loader-specific call site from code that has to
     * compile for NeoForge, Forge, Fabric and Quilt alike.</p>
     */
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
