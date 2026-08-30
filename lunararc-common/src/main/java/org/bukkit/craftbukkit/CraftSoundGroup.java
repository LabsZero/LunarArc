package org.bukkit.craftbukkit;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.SoundType;
import org.bukkit.Sound;
import org.bukkit.SoundGroup;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Objects;


@SuppressWarnings("deprecation")
public final class CraftSoundGroup implements SoundGroup, com.destroystokyo.paper.block.BlockSoundGroup {
    private final SoundType handle;

    public CraftSoundGroup(@NotNull SoundType handle) {
        this.handle = Objects.requireNonNull(handle, "handle");
    }

    public SoundType getHandle() {
        return handle;
    }

    private static @NotNull Sound toBukkit(@NotNull SoundEvent event) {
        var key = BuiltInRegistries.SOUND_EVENT.getKey(event);
        if (key == null) return Sound.BLOCK_STONE_BREAK;
        String enumName = (key.getNamespace().equals("minecraft") ? key.getPath() : key.getNamespace() + "." + key.getPath())
                .toUpperCase(Locale.ROOT).replace('.', '_').replace('/', '_');
        try {
            return Sound.valueOf(enumName);
        } catch (IllegalArgumentException ignored) {


            return Sound.BLOCK_STONE_BREAK;
        }
    }

    @Override public float getVolume() { return handle.getVolume(); }
    @Override public float getPitch() { return handle.getPitch(); }
    @Override public @NotNull Sound getBreakSound() { return toBukkit(handle.getBreakSound()); }
    @Override public @NotNull Sound getStepSound() { return toBukkit(handle.getStepSound()); }
    @Override public @NotNull Sound getPlaceSound() { return toBukkit(handle.getPlaceSound()); }
    @Override public @NotNull Sound getHitSound() { return toBukkit(handle.getHitSound()); }
    @Override public @NotNull Sound getFallSound() { return toBukkit(handle.getFallSound()); }
}
