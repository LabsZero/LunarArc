package org.bukkit.craftbukkit.entity;

import java.util.Objects;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.feline.CatVariant;
import org.bukkit.DyeColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Cat;

public final class CraftCat extends CraftTameableAnimal implements Cat {
    public CraftCat(CraftServer server, net.minecraft.world.entity.animal.feline.Cat entity) {
        super(server, entity);
    }

    @Override
    public net.minecraft.world.entity.animal.feline.Cat getHandle() {
        return (net.minecraft.world.entity.animal.feline.Cat) this.entity;
    }

    @Override
    public Type getCatType() {
        Holder<CatVariant> holder = getHandle().getVariant();
        Identifier location = holder.unwrapKey().orElseThrow(() -> new IllegalStateException("Cat variant is not registry-backed")).identifier();
        Type type = Registry.CAT_VARIANT.get(new NamespacedKey(location.getNamespace(), location.getPath()));
        if (type == null) throw new IllegalStateException("No Bukkit cat type for " + location);
        return type;
    }

    @Override
    public void setCatType(Type type) {
        Objects.requireNonNull(type, "type");
        NamespacedKey key = type.getKey();
        Identifier location = Identifier.fromNamespaceAndPath(key.getNamespace(), key.getKey());
        ResourceKey<CatVariant> resourceKey = ResourceKey.create(Registries.CAT_VARIANT, location);
        Holder.Reference<CatVariant> holder = server.getServer().registryAccess().lookupOrThrow(Registries.CAT_VARIANT)
                .get(resourceKey).orElseThrow(() -> new IllegalArgumentException("Unknown cat type " + key));
        getHandle().setVariant(holder);
    }

    @Override
    public DyeColor getCollarColor() {
        return DyeColor.getByWoolData((byte) getHandle().getCollarColor().getId());
    }

    @Override
    public void setCollarColor(DyeColor color) {
        Objects.requireNonNull(color, "color");
        getHandle().setCollarColor(net.minecraft.world.item.DyeColor.byId(color.getWoolData()));
    }

    @Override
    public void setLyingDown(boolean lyingDown) {
        getHandle().setLying(lyingDown);
    }

    @Override
    public boolean isLyingDown() {
        return getHandle().isLying();
    }

    @Override
    public void setHeadUp(boolean headUp) {
        getHandle().setRelaxStateOne(headUp);
    }

    @Override
    public boolean isHeadUp() {
        return getHandle().isRelaxStateOne();
    }

    @Override
    public String toString() {
        return "CraftCat";
    }

    public static class CraftType extends io.papermc.paper.util.OldEnumHolderable<Type, net.minecraft.world.entity.animal.feline.CatVariant> implements Type {
        private static int count = 0;

        public static Type minecraftHolderToBukkit(net.minecraft.core.Holder<net.minecraft.world.entity.animal.feline.CatVariant> minecraft) {
            return org.bukkit.craftbukkit.CraftRegistry.minecraftHolderToBukkit(minecraft, net.minecraft.core.registries.Registries.CAT_VARIANT);
        }

        public static net.minecraft.core.Holder<net.minecraft.world.entity.animal.feline.CatVariant> bukkitToMinecraftHolder(Type bukkit) {
            return org.bukkit.craftbukkit.CraftRegistry.bukkitToMinecraftHolder(bukkit);
        }

        public CraftType(final net.minecraft.core.Holder<net.minecraft.world.entity.animal.feline.CatVariant> holder) {
            super(holder, count++);
        }
    }

    @Override
    public Cat.SoundVariant getSoundVariant() {
        return CraftSoundVariant.minecraftHolderToBukkit(this.getHandle().getSoundVariant());
    }

    @Override
    public void setSoundVariant(Cat.SoundVariant soundVariant) {
        com.google.common.base.Preconditions.checkArgument(soundVariant != null, "soundVariant cannot be null");
        this.getHandle().setSoundVariant(CraftSoundVariant.bukkitToMinecraftHolder(soundVariant));
    }

    public static class CraftSoundVariant extends io.papermc.paper.registry.HolderableBase<net.minecraft.world.entity.animal.feline.CatSoundVariant> implements Cat.SoundVariant {

        public static Cat.SoundVariant minecraftHolderToBukkit(net.minecraft.core.Holder<net.minecraft.world.entity.animal.feline.CatSoundVariant> minecraft) {
            return org.bukkit.craftbukkit.CraftRegistry.minecraftHolderToBukkit(minecraft, net.minecraft.core.registries.Registries.CAT_SOUND_VARIANT);
        }

        public static net.minecraft.core.Holder<net.minecraft.world.entity.animal.feline.CatSoundVariant> bukkitToMinecraftHolder(Cat.SoundVariant bukkit) {
            return org.bukkit.craftbukkit.CraftRegistry.bukkitToMinecraftHolder(bukkit);
        }

        public CraftSoundVariant(final net.minecraft.core.Holder<net.minecraft.world.entity.animal.feline.CatSoundVariant> holder) {
            super(holder);
        }
    }
}
