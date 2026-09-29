package io.lunararcdevs.lunararc.common.mixin.core.world.blockentity;

import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.JukeboxSongPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import org.bukkit.craftbukkit.CraftRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(JukeboxBlockEntity.class)
public abstract class JukeboxBlockEntityMixin {
    @Shadow private ItemStack item;
    @Shadow @org.spongepowered.asm.mixin.Final private JukeboxSongPlayer jukeboxSongPlayer;

    public void setSongItemWithoutPlaying(ItemStack stack, long ticksSinceSongStarted) {
        BlockEntity self = (BlockEntity) (Object) this;
        this.item = stack;
        RegistryAccess access = self.getLevel() != null ? self.getLevel().registryAccess() : CraftRegistry.getMinecraftRegistry();
        JukeboxSong.fromStack(access, stack)
                .ifPresent(song -> this.jukeboxSongPlayer.setSongWithoutPlaying(song, ticksSinceSongStarted));
        if (self.getLevel() != null) {
            self.getLevel().updateNeighborsAt(self.getBlockPos(), self.getBlockState().getBlock());
        }
        self.setChanged();
    }
}
