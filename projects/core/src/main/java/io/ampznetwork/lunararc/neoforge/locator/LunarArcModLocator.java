package io.ampznetwork.lunararc.neoforge.locator;

import net.neoforged.neoforgespi.locating.IModFile;
import net.neoforged.neoforgespi.locating.IModLocator;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Registered via ServiceLoader so FML discovers LunarArc when the self-JAR is
 * on the system classloader (injected by LunarArcAgent in same-JVM mode).
 *
 * scanMods() returns empty — actual loading happens via the fml.modsDir system
 * property set in NeoForgeLauncher. Returning mods directly requires FML-internal
 * SecureJar/ModFile construction and is deferred to a future release.
 */
public class LunarArcModLocator implements IModLocator {

    @Override
    public List<ModFileOrException> scanMods() {
        return List.of();
    }

    @Override
    public String name() {
        return "lunararc-locator";
    }

    @Override
    public void scanFile(IModFile modFile, Consumer<Path> pathConsumer) {
    }

    @Override
    public void initArguments(Map<String, ?> arguments) {
    }

    @Override
    public boolean isValid(IModFile modFile) {
        return true;
    }
}
