package io.ampznetwork.lunararc.common.bridge.access;

import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Normal runtime bridge implemented by the corresponding Mixin accessor/invoker. */
public interface PrimaryLevelDataAccessBridge {
    LevelSettings lunararc$getSettings();
    void lunararc$setSettings(LevelSettings settings);
}
