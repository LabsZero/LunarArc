package io.papermc.paper.util;

import io.papermc.paper.adventure.PaperAdventure;
import io.papermc.paper.scoreboard.numbers.NumberFormat;

/** Concrete conversion between Paper number formats and Minecraft 1.21.1 number formats. */
public final class PaperScoreboardFormat {
    private PaperScoreboardFormat() {}

    public static net.minecraft.network.chat.numbers.NumberFormat asVanilla(NumberFormat format) {
        if (format instanceof io.papermc.paper.scoreboard.numbers.StyledFormat styled) {
            return new net.minecraft.network.chat.numbers.StyledFormat(PaperAdventure.asVanilla(styled.style()));
        }
        if (format instanceof io.papermc.paper.scoreboard.numbers.FixedFormat fixed) {
            return new net.minecraft.network.chat.numbers.FixedFormat(PaperAdventure.asVanilla(fixed.component()));
        }
        if (format.equals(NumberFormat.blank())) return net.minecraft.network.chat.numbers.BlankFormat.INSTANCE;
        throw new IllegalArgumentException("Unknown Paper number format " + format.getClass().getName());
    }

    public static NumberFormat asPaper(net.minecraft.network.chat.numbers.NumberFormat format) {
        if (format instanceof net.minecraft.network.chat.numbers.StyledFormat styled) {
            return NumberFormat.styled(PaperAdventure.asAdventure(styled.style));
        }
        if (format instanceof net.minecraft.network.chat.numbers.FixedFormat fixed) {
            return NumberFormat.fixed(PaperAdventure.asAdventure(fixed.value));
        }
        if (format instanceof net.minecraft.network.chat.numbers.BlankFormat) return NumberFormat.blank();
        throw new IllegalArgumentException("Unknown Minecraft number format " + format.getClass().getName());
    }
}
