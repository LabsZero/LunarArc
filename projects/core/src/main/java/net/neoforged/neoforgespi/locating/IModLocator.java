package net.neoforged.neoforgespi.locating;

import java.util.List;

/** Compile-only stub — replaced at runtime by FancyModLoader's IModLocator. */
public interface IModLocator extends IModProvider {
    List<ModFileOrException> scanMods();

    record ModFileOrException(IModFile file, Exception ex) {
        public ModFileOrException(IModFile file) {
            this(file, null);
        }
    }
}
