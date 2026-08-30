package io.ampznetwork.lunararc.common.mixin.core.command;

import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/**
 * Narrow Brigadier accessor used only to implement Paper's command replacement
 * semantics without patching/replacing Brigadier's command tree.
 */
@Mixin(CommandNode.class)
public interface CommandNodeAccessor<S> extends io.ampznetwork.lunararc.common.bridge.access.CommandNodeAccessBridge<S> {
    @Accessor("children")
    Map<String, CommandNode<S>> lunararc$getChildren();

    @Accessor("literals")
    Map<String, LiteralCommandNode<S>> lunararc$getLiterals();

    @Accessor("arguments")
    Map<String, ArgumentCommandNode<S, ?>> lunararc$getArguments();

}
