package io.lunararcdevs.lunararc.common.server;

import io.papermc.paper.plugin.lifecycle.event.registrar.Registrar;
import io.papermc.paper.plugin.lifecycle.event.registrar.RegistrarEvent;
import java.util.Objects;

/** Concrete Paper registrar event for lifecycle events that fire once and cannot be reloaded. */
public final class LunarArcRegistrarEvent<R extends Registrar> implements RegistrarEvent<R> {
    private final R registrar;

    public LunarArcRegistrarEvent(R registrar) {
        this.registrar = Objects.requireNonNull(registrar, "registrar");
    }

    @Override
    public R registrar() {
        return registrar;
    }
}
