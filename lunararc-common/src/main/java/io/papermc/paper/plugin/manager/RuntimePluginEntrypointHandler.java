package io.papermc.paper.plugin.manager;

import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.entrypoint.Entrypoint;
import io.papermc.paper.plugin.entrypoint.EntrypointHandler;
import io.papermc.paper.plugin.provider.PluginProvider;
import io.papermc.paper.plugin.storage.BootstrapProviderStorage;
import io.papermc.paper.plugin.storage.ProviderStorage;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

/**
 * Used for loading plugins during runtime.
 *
 * LunarArc note: real Paper discovers and registers providers for both entrypoints
 * (BOOTSTRAPPER, PLUGIN) very early in boot (Main.java, before Bootstrap.bootStrap() even runs)
 * via LaunchEntryPointHandler, then enters BOOTSTRAPPER and PLUGIN separately, much later, at
 * their own real call sites. LunarArc cannot reliably hook that early injection point via Mixin
 * (confirmed unreliable across multiple injection points in an earlier session), so this merges
 * discovery and both entrypoints into a single call at boot instead - see
 * PaperPluginInstanceManager.loadPlugins(File[]/Path), which registers everything through one of
 * these handlers and then enters BOOTSTRAPPER before PLUGIN, same relative order real Paper uses.
 * This class originally only accepted/entered Entrypoint.PLUGIN and threw for anything else,
 * which meant PluginFileType.PAPER's own registration of a plugin's bootstrapper into
 * Entrypoint.BOOTSTRAPPER (io.papermc.paper.plugin.provider.type.PluginFileType.PAPER.register())
 * threw immediately - so any paper-plugin.yml plugin declaring a bootstrapper: class failed to
 * load entirely, not merely skipped its bootstrap phase.
 */
class RuntimePluginEntrypointHandler<T extends ProviderStorage<JavaPlugin>> implements EntrypointHandler {

    private final T providerStorage;
    private final BootstrapProviderStorage bootstrapProviderStorage = new BootstrapProviderStorage();

    RuntimePluginEntrypointHandler(T providerStorage) {
        this.providerStorage = providerStorage;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> void register(Entrypoint<T> entrypoint, PluginProvider<T> provider) {
        if (entrypoint == Entrypoint.BOOTSTRAPPER) {
            this.bootstrapProviderStorage.register((PluginProvider<PluginBootstrap>) provider);
            return;
        }

        this.providerStorage.register((PluginProvider<JavaPlugin>) provider);
    }

    @Override
    public void enter(Entrypoint<?> entrypoint) {
        if (entrypoint == Entrypoint.BOOTSTRAPPER) {
            this.bootstrapProviderStorage.enter();
            return;
        }
        if (entrypoint != Entrypoint.PLUGIN) {
            throw new IllegalArgumentException("Only bootstrapper/plugin entrypoints supported");
        }
        this.providerStorage.enter();
    }

    @NotNull
    public T getPluginProviderStorage() {
        return this.providerStorage;
    }
}
