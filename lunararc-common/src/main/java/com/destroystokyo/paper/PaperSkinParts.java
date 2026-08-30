package com.destroystokyo.paper;

import java.util.StringJoiner;


public final class PaperSkinParts implements SkinParts {
    private final int raw;

    public PaperSkinParts(int raw) {
        this.raw = raw;
    }

    @Override public boolean hasCapeEnabled() { return (raw & 1) == 1; }
    @Override public boolean hasJacketEnabled() { return (raw >> 1 & 1) == 1; }
    @Override public boolean hasLeftSleeveEnabled() { return (raw >> 2 & 1) == 1; }
    @Override public boolean hasRightSleeveEnabled() { return (raw >> 3 & 1) == 1; }
    @Override public boolean hasLeftPantsEnabled() { return (raw >> 4 & 1) == 1; }
    @Override public boolean hasRightPantsEnabled() { return (raw >> 5 & 1) == 1; }
    @Override public boolean hasHatsEnabled() { return (raw >> 6 & 1) == 1; }
    @Override public int getRaw() { return raw; }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof PaperSkinParts that && raw == that.raw);
    }

    @Override public int hashCode() { return Integer.hashCode(raw); }

    @Override
    public String toString() {
        return new StringJoiner(", ", "PaperSkinParts[", "]")
                .add("raw=" + raw)
                .add("cape=" + hasCapeEnabled())
                .add("jacket=" + hasJacketEnabled())
                .add("leftSleeve=" + hasLeftSleeveEnabled())
                .add("rightSleeve=" + hasRightSleeveEnabled())
                .add("leftPants=" + hasLeftPantsEnabled())
                .add("rightPants=" + hasRightPantsEnabled())
                .add("hats=" + hasHatsEnabled())
                .toString();
    }
}
