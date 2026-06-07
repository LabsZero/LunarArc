package net.neoforged.neoforgespi.locating;

import java.nio.file.Path;
import java.util.Map;
import java.util.function.Consumer;

/** Compile-only stub — replaced at runtime by FancyModLoader's IModProvider. */
public interface IModProvider {
    String name();
    void scanFile(IModFile modFile, Consumer<Path> pathConsumer);
    void initArguments(Map<String, ?> arguments);
    boolean isValid(IModFile modFile);
}
