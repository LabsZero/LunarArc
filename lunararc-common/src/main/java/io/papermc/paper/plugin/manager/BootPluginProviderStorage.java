package io.papermc.paper.plugin.manager;

import io.papermc.paper.plugin.entrypoint.dependency.MetaDependencyTree;

/**
 * Boot-time plugin provider storage.
 *
 * <p>Identical to {@link MultiRuntimePluginProviderStorage} except that it accepts
 * paper-plugin.yml providers. Real Paper never needs this because it runs discovery and the
 * BOOTSTRAPPER entrypoint from {@code Main}/{@code Bootstrap}, long before {@code CraftServer}
 * exists, and only enters PLUGIN from {@code CraftServer#loadPlugins} via
 * {@code LaunchEntryPointHandler}. LunarArc cannot hook that early injection point reliably, so
 * it enters all three from {@code CraftServer#loadPlugins} using the runtime storage - which
 * inherited Paper's "no paper plugins at runtime" refusal and silently dropped every modern
 * plugin at boot ("Skipping loading of paper plugin requested from SimplePluginManager"). The
 * plugin's bootstrapper had already run by then, since BOOTSTRAPPER registrations go to a
 * separate storage, so the refusal discarded a plugin that was ready to load.</p>
 */
public class BootPluginProviderStorage extends MultiRuntimePluginProviderStorage {

    BootPluginProviderStorage(MetaDependencyTree dependencyTree) {
        super(dependencyTree);
    }

    @Override
    protected boolean skipsPaperPlugins() {
        return false;
    }
}
