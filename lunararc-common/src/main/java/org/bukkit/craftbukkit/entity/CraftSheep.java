package org.bukkit.craftbukkit.entity;
import java.util.Objects;
import org.bukkit.DyeColor;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Sheep;
public final class CraftSheep extends CraftAnimals implements Sheep {
    public CraftSheep(CraftServer server, net.minecraft.world.entity.animal.Sheep entity){super(server,entity);}
    @Override public DyeColor getColor(){return DyeColor.getByWoolData((byte)getHandle().getColor().getId());}
    @Override public void setColor(DyeColor color){Objects.requireNonNull(color,"color");getHandle().setColor(net.minecraft.world.item.DyeColor.byId(color.getWoolData()));}
    @Override public boolean isSheared(){return getHandle().isSheared();}
    @Override public void setSheared(boolean flag){getHandle().setSheared(flag);}
    @Override public boolean readyToBeSheared(){return getHandle().readyForShearing();}
    @Override public void shear(net.kyori.adventure.sound.Sound.Source source) {
        Objects.requireNonNull(source, "source");
        net.minecraft.sounds.SoundSource vanillaSource = switch (source) {
            case MASTER -> net.minecraft.sounds.SoundSource.MASTER;
            case MUSIC -> net.minecraft.sounds.SoundSource.MUSIC;
            case RECORD -> net.minecraft.sounds.SoundSource.RECORDS;
            case WEATHER -> net.minecraft.sounds.SoundSource.WEATHER;
            case BLOCK -> net.minecraft.sounds.SoundSource.BLOCKS;
            case HOSTILE -> net.minecraft.sounds.SoundSource.HOSTILE;
            case NEUTRAL -> net.minecraft.sounds.SoundSource.NEUTRAL;
            case PLAYER -> net.minecraft.sounds.SoundSource.PLAYERS;
            case AMBIENT -> net.minecraft.sounds.SoundSource.AMBIENT;
            case VOICE -> net.minecraft.sounds.SoundSource.VOICE;
        };
        getHandle().shear(vanillaSource);
    }
    @Override public net.minecraft.world.entity.animal.Sheep getHandle(){return (net.minecraft.world.entity.animal.Sheep)this.entity;}
}
