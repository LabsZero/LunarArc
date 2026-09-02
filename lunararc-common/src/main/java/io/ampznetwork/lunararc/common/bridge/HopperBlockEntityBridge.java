package io.ampznetwork.lunararc.common.bridge;

/**
 * Access to {@code HopperBlockEntity}'s private cooldown.
 *
 * <p>{@code setCooldown} is private in 1.21.1, and the code that needs it is a static handler that
 * has a hopper in hand rather than being one - so {@code this.setCooldown(...)} is not available to
 * it. Shadowing the method inside the mixin and exposing it here is the same route this project
 * already takes for ServerPlayer's private nextContainerCounter and initMenu, and it needs no
 * access widener or access transformer entry, so it reads the same on all four loaders.</p>
 */
public interface HopperBlockEntityBridge {

    /** Delay this hopper's next transfer attempt by the given number of ticks. */
    void lunararc$setCooldown(int cooldown);
}
