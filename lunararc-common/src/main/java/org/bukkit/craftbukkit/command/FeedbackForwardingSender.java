package org.bukkit.craftbukkit.command;

import java.util.Objects;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import org.bukkit.Server;
import org.bukkit.craftbukkit.CraftServer;
import org.jetbrains.annotations.NotNull;

public final class FeedbackForwardingSender extends ServerCommandSender {
    private final Consumer<? super Component> feedback;
    private final CraftServer server;

    public FeedbackForwardingSender(Consumer<? super Component> feedback, CraftServer server) {
        this.feedback = Objects.requireNonNull(feedback, "feedback");
        this.server = Objects.requireNonNull(server, "server");
    }

    @Override
    public void sendMessage(@NotNull String message) {
        this.feedback.accept(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(message));
    }

    @Override
    public void sendMessage(@NotNull String... messages) {
        for (String message : messages) {
            this.sendMessage(message);
        }
    }

    @Override
    public void sendMessage(Component message) {
        this.feedback.accept(Objects.requireNonNull(message, "message"));
    }

    @Override
    public @NotNull String getName() {
        return "FeedbackForwardingSender";
    }

    @Override
    public @NotNull Component name() {
        return Component.text(this.getName());
    }

    @Override
    public boolean isOp() {
        return true;
    }

    @Override
    public void setOp(boolean value) {
        throw new UnsupportedOperationException("Cannot change operator status of " + this.getClass().getName());
    }

    @Override
    public @NotNull Server getServer() {
        return this.server;
    }
}
