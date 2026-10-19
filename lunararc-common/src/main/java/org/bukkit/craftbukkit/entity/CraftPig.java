package org.bukkit.craftbukkit.entity;

import io.lunararcdevs.lunararc.common.bridge.entity.ItemBasedSteeringBridge;
import io.lunararcdevs.lunararc.common.bridge.access.PigAccessBridge;
import org.bukkit.Material;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Pig;

public final class CraftPig extends CraftAnimals implements Pig {
    public CraftPig(CraftServer server, net.minecraft.world.entity.animal.pig.Pig entity) {
        super(server, entity);
    }

    @Override public net.minecraft.world.entity.animal.pig.Pig getHandle() {
        return (net.minecraft.world.entity.animal.pig.Pig) this.entity;
    }

    private net.minecraft.world.entity.ItemBasedSteering steering() {
        return ((PigAccessBridge) (Object) getHandle()).lunararc$getSteering();
    }

    private ItemBasedSteeringBridge steeringBridge() {
        return (ItemBasedSteeringBridge) (Object) steering();
    }

    @Override public boolean hasSaddle() { return getHandle().isSaddled(); }
    @Override public void setSaddle(boolean saddled) {
        getHandle().setItemSlot(net.minecraft.world.entity.EquipmentSlot.SADDLE,
                saddled ? new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SADDLE) : net.minecraft.world.item.ItemStack.EMPTY);
    }

    @Override
    public Variant getVariant() {
        return CraftVariant.minecraftHolderToBukkit(this.getHandle().getVariant());
    }

    @Override
    public void setVariant(Variant variant) {
        com.google.common.base.Preconditions.checkArgument(variant != null, "variant cannot be null");
        this.getHandle().setVariant(CraftVariant.bukkitToMinecraftHolder(variant));
    }

    public static class CraftVariant extends io.papermc.paper.registry.HolderableBase<net.minecraft.world.entity.animal.pig.PigVariant> implements Variant {

        public static Variant minecraftHolderToBukkit(net.minecraft.core.Holder<net.minecraft.world.entity.animal.pig.PigVariant> minecraft) {
            return org.bukkit.craftbukkit.CraftRegistry.minecraftHolderToBukkit(minecraft, net.minecraft.core.registries.Registries.PIG_VARIANT);
        }

        public static net.minecraft.core.Holder<net.minecraft.world.entity.animal.pig.PigVariant> bukkitToMinecraftHolder(Variant bukkit) {
            return org.bukkit.craftbukkit.CraftRegistry.bukkitToMinecraftHolder(bukkit);
        }

        public CraftVariant(final net.minecraft.core.Holder<net.minecraft.world.entity.animal.pig.PigVariant> holder) {
            super(holder);
        }
    }


    @Override
    public int getBoostTicks() {
        return steeringBridge().lunararc$isBoosting() ? steeringBridge().lunararc$getBoostTimeTotal() : 0;
    }

    @Override
    public void setBoostTicks(int ticks) {
        if (ticks < 0) throw new IllegalArgumentException("ticks must be >= 0");
        steeringBridge().lunararc$setBoostTicks(ticks);
    }

    @Override
    public int getCurrentBoostTicks() {
        return steeringBridge().lunararc$isBoosting() ? steeringBridge().lunararc$getBoostTime() : 0;
    }

    @Override
    public void setCurrentBoostTicks(int ticks) {
        ItemBasedSteeringBridge steering = steeringBridge();
        if (!steering.lunararc$isBoosting()) return;
        int max = steering.lunararc$getBoostTimeTotal();
        if (ticks < 0 || ticks > max) throw new IllegalArgumentException("boost ticks must be between 0 and " + max);
        steering.lunararc$setBoostTime(ticks);
    }

    @Override public Material getSteerMaterial() { return Material.CARROT_ON_A_STICK; }
    @Override public String toString() { return "CraftPig"; }

    @Override
    public Pig.SoundVariant getSoundVariant() {
        return CraftSoundVariant.minecraftHolderToBukkit(this.getHandle().getSoundVariant());
    }

    @Override
    public void setSoundVariant(Pig.SoundVariant soundVariant) {
        com.google.common.base.Preconditions.checkArgument(soundVariant != null, "soundVariant cannot be null");
        this.getHandle().setSoundVariant(CraftSoundVariant.bukkitToMinecraftHolder(soundVariant));
    }

    public static class CraftSoundVariant extends io.papermc.paper.registry.HolderableBase<net.minecraft.world.entity.animal.pig.PigSoundVariant> implements Pig.SoundVariant {

        public static Pig.SoundVariant minecraftHolderToBukkit(net.minecraft.core.Holder<net.minecraft.world.entity.animal.pig.PigSoundVariant> minecraft) {
            return org.bukkit.craftbukkit.CraftRegistry.minecraftHolderToBukkit(minecraft, net.minecraft.core.registries.Registries.PIG_SOUND_VARIANT);
        }

        public static net.minecraft.core.Holder<net.minecraft.world.entity.animal.pig.PigSoundVariant> bukkitToMinecraftHolder(Pig.SoundVariant bukkit) {
            return org.bukkit.craftbukkit.CraftRegistry.bukkitToMinecraftHolder(bukkit);
        }

        public CraftSoundVariant(final net.minecraft.core.Holder<net.minecraft.world.entity.animal.pig.PigSoundVariant> holder) {
            super(holder);
        }
    }
}
