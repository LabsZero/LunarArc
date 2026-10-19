package org.bukkit.craftbukkit.entity;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Cow;
public class CraftCow extends CraftAnimals implements Cow {
    public CraftCow(CraftServer server, net.minecraft.world.entity.animal.cow.Cow entity){super(server,entity);}
    @Override public net.minecraft.world.entity.animal.cow.Cow getHandle(){return (net.minecraft.world.entity.animal.cow.Cow)this.entity;}

    @Override
    public Variant getVariant() {
        return CraftVariant.minecraftHolderToBukkit(this.getHandle().getVariant());
    }

    @Override
    public void setVariant(Variant variant) {
        com.google.common.base.Preconditions.checkArgument(variant != null, "variant cannot be null");
        this.getHandle().setVariant(CraftVariant.bukkitToMinecraftHolder(variant));
    }

    public static class CraftVariant extends io.papermc.paper.registry.HolderableBase<net.minecraft.world.entity.animal.cow.CowVariant> implements Variant {

        public static Variant minecraftHolderToBukkit(net.minecraft.core.Holder<net.minecraft.world.entity.animal.cow.CowVariant> minecraft) {
            return org.bukkit.craftbukkit.CraftRegistry.minecraftHolderToBukkit(minecraft, net.minecraft.core.registries.Registries.COW_VARIANT);
        }

        public static net.minecraft.core.Holder<net.minecraft.world.entity.animal.cow.CowVariant> bukkitToMinecraftHolder(Variant bukkit) {
            return org.bukkit.craftbukkit.CraftRegistry.bukkitToMinecraftHolder(bukkit);
        }

        public CraftVariant(final net.minecraft.core.Holder<net.minecraft.world.entity.animal.cow.CowVariant> holder) {
            super(holder);
        }
    }

    @Override
    public Cow.SoundVariant getSoundVariant() {
        return CraftSoundVariant.minecraftHolderToBukkit(this.getHandle().getSoundVariant());
    }

    @Override
    public void setSoundVariant(Cow.SoundVariant soundVariant) {
        com.google.common.base.Preconditions.checkArgument(soundVariant != null, "soundVariant cannot be null");
        this.getHandle().setSoundVariant(CraftSoundVariant.bukkitToMinecraftHolder(soundVariant));
    }

    public static class CraftSoundVariant extends io.papermc.paper.registry.HolderableBase<net.minecraft.world.entity.animal.cow.CowSoundVariant> implements Cow.SoundVariant {

        public static Cow.SoundVariant minecraftHolderToBukkit(net.minecraft.core.Holder<net.minecraft.world.entity.animal.cow.CowSoundVariant> minecraft) {
            return org.bukkit.craftbukkit.CraftRegistry.minecraftHolderToBukkit(minecraft, net.minecraft.core.registries.Registries.COW_SOUND_VARIANT);
        }

        public static net.minecraft.core.Holder<net.minecraft.world.entity.animal.cow.CowSoundVariant> bukkitToMinecraftHolder(Cow.SoundVariant bukkit) {
            return org.bukkit.craftbukkit.CraftRegistry.bukkitToMinecraftHolder(bukkit);
        }

        public CraftSoundVariant(final net.minecraft.core.Holder<net.minecraft.world.entity.animal.cow.CowSoundVariant> holder) {
            super(holder);
        }
    }
}
