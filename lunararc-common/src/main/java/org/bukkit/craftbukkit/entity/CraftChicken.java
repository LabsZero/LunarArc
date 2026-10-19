package org.bukkit.craftbukkit.entity;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Chicken;
public final class CraftChicken extends CraftAnimals implements Chicken {
    public CraftChicken(CraftServer server, net.minecraft.world.entity.animal.chicken.Chicken entity){super(server,entity);}
    @Override public boolean isChickenJockey(){return getHandle().isChickenJockey();}
    @Override public void setIsChickenJockey(boolean value){getHandle().setChickenJockey(value);}
    @Override public int getEggLayTime(){return getHandle().eggTime;}
    @Override public void setEggLayTime(int ticks){getHandle().eggTime = ticks;}
    @Override public net.minecraft.world.entity.animal.chicken.Chicken getHandle(){return (net.minecraft.world.entity.animal.chicken.Chicken)this.entity;}

    @Override
    public Variant getVariant() {
        return CraftVariant.minecraftHolderToBukkit(this.getHandle().getVariant());
    }

    @Override
    public void setVariant(Variant variant) {
        com.google.common.base.Preconditions.checkArgument(variant != null, "variant cannot be null");
        this.getHandle().setVariant(CraftVariant.bukkitToMinecraftHolder(variant));
    }

    public static class CraftVariant extends io.papermc.paper.registry.HolderableBase<net.minecraft.world.entity.animal.chicken.ChickenVariant> implements Variant {

        public static Variant minecraftHolderToBukkit(net.minecraft.core.Holder<net.minecraft.world.entity.animal.chicken.ChickenVariant> minecraft) {
            return org.bukkit.craftbukkit.CraftRegistry.minecraftHolderToBukkit(minecraft, net.minecraft.core.registries.Registries.CHICKEN_VARIANT);
        }

        public static net.minecraft.core.Holder<net.minecraft.world.entity.animal.chicken.ChickenVariant> bukkitToMinecraftHolder(Variant bukkit) {
            return org.bukkit.craftbukkit.CraftRegistry.bukkitToMinecraftHolder(bukkit);
        }

        public CraftVariant(final net.minecraft.core.Holder<net.minecraft.world.entity.animal.chicken.ChickenVariant> holder) {
            super(holder);
        }
    }

    @Override
    public Chicken.SoundVariant getSoundVariant() {
        return CraftSoundVariant.minecraftHolderToBukkit(this.getHandle().getSoundVariant());
    }

    @Override
    public void setSoundVariant(Chicken.SoundVariant soundVariant) {
        com.google.common.base.Preconditions.checkArgument(soundVariant != null, "soundVariant cannot be null");
        this.getHandle().setSoundVariant(CraftSoundVariant.bukkitToMinecraftHolder(soundVariant));
    }

    public static class CraftSoundVariant extends io.papermc.paper.registry.HolderableBase<net.minecraft.world.entity.animal.chicken.ChickenSoundVariant> implements Chicken.SoundVariant {

        public static Chicken.SoundVariant minecraftHolderToBukkit(net.minecraft.core.Holder<net.minecraft.world.entity.animal.chicken.ChickenSoundVariant> minecraft) {
            return org.bukkit.craftbukkit.CraftRegistry.minecraftHolderToBukkit(minecraft, net.minecraft.core.registries.Registries.CHICKEN_SOUND_VARIANT);
        }

        public static net.minecraft.core.Holder<net.minecraft.world.entity.animal.chicken.ChickenSoundVariant> bukkitToMinecraftHolder(Chicken.SoundVariant bukkit) {
            return org.bukkit.craftbukkit.CraftRegistry.bukkitToMinecraftHolder(bukkit);
        }

        public CraftSoundVariant(final net.minecraft.core.Holder<net.minecraft.world.entity.animal.chicken.ChickenSoundVariant> holder) {
            super(holder);
        }
    }
}
