package io.papermc.paper.registry.event;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEventType;
import io.papermc.paper.plugin.lifecycle.event.types.PrioritizableLifecycleEventType;
import io.papermc.paper.registry.RegistryBuilder;
import io.papermc.paper.registry.event.type.RegistryEntryAddEventType;
import io.papermc.paper.registry.event.type.RegistryEntryAddEventTypeImpl;

public final class RegistryEventTypeProviderImpl implements RegistryEventTypeProvider {
    @Override
    public <T, B extends RegistryBuilder<T>> RegistryEntryAddEventType<T, B> registryEntryAdd(RegistryEventProvider<T, B> provider) {
        return new RegistryEntryAddEventTypeImpl<>(provider.registryKey(), "registry_entry_add:" + provider.registryKey());
    }

    @Override
    public <T, B extends RegistryBuilder<T>> LifecycleEventType.Prioritizable<BootstrapContext, RegistryFreezeEvent<T, B>> registryFreeze(
            RegistryEventProvider<T, B> provider) {
        return new PrioritizableLifecycleEventType.Simple<>("registry_freeze:" + provider.registryKey(), BootstrapContext.class);
    }
}
