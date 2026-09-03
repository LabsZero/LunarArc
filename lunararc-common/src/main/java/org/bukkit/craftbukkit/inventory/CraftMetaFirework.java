package org.bukkit.craftbukkit.inventory;

import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.inventory.meta.FireworkMeta;

/** Concrete firework-rocket metadata over Minecraft 1.21.1 FIREWORKS. */
public final class CraftMetaFirework extends CraftItemMeta implements FireworkMeta {
    private List<FireworkEffect> effects;
    private Integer power;

    public CraftMetaFirework() { super(); }
    public CraftMetaFirework(ItemStack nms) {
        super(nms);
        if (nms == null || nms.isEmpty()) return;
        Fireworks fireworks = nms.get(DataComponents.FIREWORKS);
        if (fireworks == null) return;
        this.power = fireworks.flightDuration();
        this.effects = new ArrayList<>();
        for (FireworkExplosion explosion : fireworks.explosions()) this.effects.add(fromExplosion(explosion));
    }

    @Override public void applyToNms(ItemStack nms) {
        super.applyToNms(nms);
        if (this.power == null && this.effects == null) { nms.remove(DataComponents.FIREWORKS); return; }
        List<FireworkExplosion> explosions = new ArrayList<>();
        if (this.effects != null) for (FireworkEffect effect : this.effects) explosions.add(toExplosion(effect));
        nms.set(DataComponents.FIREWORKS, new Fireworks(getPower(), explosions));
    }

    static FireworkEffect fromExplosion(FireworkExplosion explosion) {
        FireworkEffect.Builder b = FireworkEffect.builder().flicker(explosion.hasTwinkle()).trail(explosion.hasTrail()).with(switch (explosion.shape()) {
            case SMALL_BALL -> FireworkEffect.Type.BALL;
            case LARGE_BALL -> FireworkEffect.Type.BALL_LARGE;
            case STAR -> FireworkEffect.Type.STAR;
            case CREEPER -> FireworkEffect.Type.CREEPER;
            case BURST -> FireworkEffect.Type.BURST;
        });
        for (int color : explosion.colors()) b.withColor(Color.fromRGB(color & 0xFFFFFF));
        for (int color : explosion.fadeColors()) b.withFade(Color.fromRGB(color & 0xFFFFFF));
        return b.build();
    }

    static FireworkExplosion toExplosion(FireworkEffect effect) {
        return new FireworkExplosion(switch (effect.getType()) {
            case BALL -> FireworkExplosion.Shape.SMALL_BALL;
            case BALL_LARGE -> FireworkExplosion.Shape.LARGE_BALL;
            case STAR -> FireworkExplosion.Shape.STAR;
            case CREEPER -> FireworkExplosion.Shape.CREEPER;
            case BURST -> FireworkExplosion.Shape.BURST;
        }, colors(effect.getColors()), colors(effect.getFadeColors()), effect.hasTrail(), effect.hasFlicker());
    }

    private static IntList colors(List<Color> colors) {
        int[] values = new int[colors.size()];
        for (int i=0;i<colors.size();i++) values[i]=colors.get(i).asRGB();
        return IntList.of(values);
    }

    @Override public boolean hasEffects() { return this.effects != null && !this.effects.isEmpty(); }
    @Override public void addEffect(FireworkEffect effect) { Preconditions.checkNotNull(effect, "effect"); int size=getEffectsSize(); Preconditions.checkArgument(size < Fireworks.MAX_EXPLOSIONS, "Cannot have more than %s effects", Fireworks.MAX_EXPLOSIONS); if(this.effects==null)this.effects=new ArrayList<>(); this.effects.add(effect); }
    @Override public void addEffects(FireworkEffect... effects) { Preconditions.checkNotNull(effects,"effects"); Preconditions.checkArgument(getEffectsSize()+effects.length<=Fireworks.MAX_EXPLOSIONS,"Cannot have more than %s effects",Fireworks.MAX_EXPLOSIONS); for(FireworkEffect e:effects)addEffect(e); }
    @Override public void addEffects(Iterable<FireworkEffect> effects) { Preconditions.checkNotNull(effects,"effects"); for(FireworkEffect e:effects)addEffect(e); }
    @Override public List<FireworkEffect> getEffects() { return this.effects==null?List.of():Collections.unmodifiableList(new ArrayList<>(this.effects)); }
    @Override public int getEffectsSize() { return this.effects==null?0:this.effects.size(); }
    @Override public void removeEffect(int index) { if(this.effects==null)throw new IndexOutOfBoundsException(index); this.effects.remove(index); }
    @Override public void clearEffects() { this.effects=null; }
    @Override public boolean hasPower() { return this.power!=null; }
    @Override public int getPower() { return this.power==null?0:this.power; }
    @Override public void setPower(int power) { Preconditions.checkArgument(power>=0 && power<=255,"power must be between 0 and 255: %s",power); this.power=power; }
    @Override public CraftMetaFirework clone() { CraftMetaFirework c=(CraftMetaFirework)super.clone(); c.effects=this.effects==null?null:new ArrayList<>(this.effects); return c; }
}
