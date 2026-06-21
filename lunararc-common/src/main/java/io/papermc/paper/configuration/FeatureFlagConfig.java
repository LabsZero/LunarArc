package io.papermc.paper.configuration;

import java.util.Set;

public interface FeatureFlagConfig {
    Set<Object> getFeatureFlags();
    boolean isFeatureFlagEnabled(Object flag);
}
