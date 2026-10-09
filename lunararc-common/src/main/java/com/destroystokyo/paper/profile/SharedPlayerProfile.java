package com.destroystokyo.paper.profile;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.world.item.component.ResolvableProfile;

import java.util.UUID;

public interface SharedPlayerProfile {
    UUID getUniqueId();

    String getName();

    boolean removeProperty(String name);

    Property getProperty(String name);

    void setProperty(String name, Property property);

    GameProfile buildGameProfile();

    ResolvableProfile buildResolvableProfile();
}
