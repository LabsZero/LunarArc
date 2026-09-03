package org.bukkit.craftbukkit.inventory;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.FireworkExplosion;
import org.bukkit.FireworkEffect;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.jetbrains.annotations.Nullable;

/** Concrete firework-star metadata over Minecraft 1.21.1 FIREWORK_EXPLOSION. */
public final class CraftMetaCharge extends CraftItemMeta implements FireworkEffectMeta {
    private FireworkEffect effect;
    public CraftMetaCharge() { super(); }
    public CraftMetaCharge(ItemStack nms) { super(nms); if(nms!=null&&!nms.isEmpty()){ FireworkExplosion e=nms.get(DataComponents.FIREWORK_EXPLOSION); if(e!=null)this.effect=CraftMetaFirework.fromExplosion(e);} }
    @Override public void applyToNms(ItemStack nms) { super.applyToNms(nms); if(this.effect==null)nms.remove(DataComponents.FIREWORK_EXPLOSION); else nms.set(DataComponents.FIREWORK_EXPLOSION,CraftMetaFirework.toExplosion(this.effect)); }
    @Override public boolean hasEffect() { return this.effect!=null; }
    @Override public @Nullable FireworkEffect getEffect() { return this.effect; }
    @Override public void setEffect(@Nullable FireworkEffect effect) { this.effect=effect; }
    @Override public CraftMetaCharge clone(){ return (CraftMetaCharge)super.clone(); }
}
