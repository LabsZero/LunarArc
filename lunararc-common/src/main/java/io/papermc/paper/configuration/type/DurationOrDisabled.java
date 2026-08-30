package io.papermc.paper.configuration.type;

import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public final class DurationOrDisabled {
    public static final DurationOrDisabled USE_DISABLED = new DurationOrDisabled(Optional.empty());

    private Optional<Duration> value;

    public DurationOrDisabled(final Optional<Duration> value) {
        this.value = value;
    }

    public Optional<Duration> value() {
        return this.value;
    }

    public void value(final Optional<Duration> value) {
        this.value = value;
    }

    public Duration or(final Duration fallback) {
        return this.value.orElse(fallback);
    }
}
