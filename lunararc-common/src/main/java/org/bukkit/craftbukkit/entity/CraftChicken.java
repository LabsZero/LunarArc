package org.bukkit.craftbukkit.entity;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Chicken;
public final class CraftChicken extends CraftAnimals implements Chicken {
    public CraftChicken(CraftServer server, net.minecraft.world.entity.animal.Chicken entity){super(server,entity);}
    @Override public boolean isChickenJockey(){return getHandle().isChickenJockey();}
    @Override public void setIsChickenJockey(boolean value){getHandle().setChickenJockey(value);}
    @Override public int getEggLayTime(){return getHandle().eggTime;}
    @Override public void setEggLayTime(int ticks){getHandle().eggTime = ticks;}
    @Override public net.minecraft.world.entity.animal.Chicken getHandle(){return (net.minecraft.world.entity.animal.Chicken)this.entity;}
}
