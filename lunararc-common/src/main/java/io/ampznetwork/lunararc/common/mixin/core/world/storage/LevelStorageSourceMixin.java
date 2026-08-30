package io.ampznetwork.lunararc.common.mixin.core.world.storage;

import io.ampznetwork.lunararc.common.bridge.storage.LevelStorageSourceBridge;
import java.io.IOException;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.validation.ContentValidationException;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LevelStorageSource.class)
public abstract class LevelStorageSourceMixin implements LevelStorageSourceBridge {
    @Shadow public abstract LevelStorageSource.LevelStorageAccess validateAndCreateAccess(String saveName)
            throws IOException, ContentValidationException;

    @Override
    public LevelStorageSource.LevelStorageAccess lunararc$validateAndCreateAccess(
            String saveName, ResourceKey<LevelStem> dimension) throws IOException, ContentValidationException {
        LevelStorageSource.LevelStorageAccess access = this.validateAndCreateAccess(saveName);
        ((LevelStorageSourceBridge.AccessBridge) (Object) access).lunararc$setDimensionType(dimension);
        return access;
    }
}
