package org.bukkit.craftbukkit.inventory.components;

import com.google.common.base.Preconditions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.EitherHolder;
import net.minecraft.world.item.JukeboxPlayable;
import org.bukkit.JukeboxSong;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.inventory.meta.components.JukeboxPlayableComponent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Paper 1.21.1 snapshot wrapper for the JUKEBOX_PLAYABLE data component. */
public final class CraftJukeboxComponent implements JukeboxPlayableComponent {
    private JukeboxPlayable handle;
    public CraftJukeboxComponent(JukeboxPlayable handle){this.handle=Objects.requireNonNull(handle,"handle");}
    public CraftJukeboxComponent(JukeboxPlayableComponent source){Objects.requireNonNull(source,"source");this.handle=new JukeboxPlayable(new EitherHolder<>(ResourceKey.create(Registries.JUKEBOX_SONG,ResourceLocation.parse(source.getSongKey().toString()))),source.isShowInTooltip());}
    public JukeboxPlayable getHandle(){return handle;}
    @Override public JukeboxSong getSong(){return Registry.JUKEBOX_SONG.get(getSongKey());}
    @Override public NamespacedKey getSongKey(){return NamespacedKey.fromString(handle.song().key().location().toString());}
    @Override public void setSong(JukeboxSong song){Preconditions.checkArgument(song!=null,"song cannot be null");setSongKey(song.getKey());}
    @Override public void setSongKey(NamespacedKey song){Preconditions.checkArgument(song!=null,"song cannot be null");handle=new JukeboxPlayable(new EitherHolder<>(ResourceKey.create(Registries.JUKEBOX_SONG,ResourceLocation.parse(song.toString()))),handle.showInTooltip());}
    @Override public boolean isShowInTooltip(){return handle.showInTooltip();}
    @Override public void setShowInTooltip(boolean show){handle=new JukeboxPlayable(handle.song(),show);}
    @Override public Map<String,Object> serialize(){Map<String,Object>m=new LinkedHashMap<>();m.put("song",getSongKey().toString());m.put("show-in-tooltip",isShowInTooltip());return m;}
    @Override public boolean equals(Object o){return o instanceof CraftJukeboxComponent c&&Objects.equals(handle,c.handle);}@Override public int hashCode(){return Objects.hashCode(handle);}
}
