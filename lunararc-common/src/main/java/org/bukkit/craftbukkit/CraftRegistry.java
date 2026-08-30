package org.bukkit.craftbukkit;

import io.ampznetwork.lunararc.common.LunarArcServerAccess;
import net.minecraft.core.RegistryAccess;

/**
 * Real Paper 1.21.1 ships a much larger {@code CraftRegistry} that also backs the
 * dynamic Registry Modification API (patches 0471, 0913, 0920, 1014 in
 * PaperMC/Paper-archive ver/1.21.1). That surface is not implemented here yet —
 * porting it is tracked separately and should not be faked.
 *
 * <p>This class only provides {@link #getMinecraftRegistry()}, the one entry point
 * {@code io.papermc.paper.adventure.WrapperAwareSerializer} needs: the live, loader-owned
 * {@link RegistryAccess} for the running server, exactly as real Paper's CraftRegistry
 * returns it. Do not add unrelated registry-modification methods here without porting the
 * real patches; a partial shim gives its own kind of false confidence.</p>
 */
public final class CraftRegistry {
    private CraftRegistry() {}

    public static RegistryAccess getMinecraftRegistry() {
        return LunarArcServerAccess.getMinecraftServer().registryAccess();
    }
}
