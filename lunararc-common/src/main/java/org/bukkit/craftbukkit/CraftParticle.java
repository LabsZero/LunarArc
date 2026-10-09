package org.bukkit.craftbukkit;

public final class CraftParticle {
    private CraftParticle() {}

    public static net.minecraft.core.particles.ParticleOptions createParticleParam(org.bukkit.Particle particle, Object data) {
        java.util.Objects.requireNonNull(particle, "particle");
        net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.parse(particle.getKey().toString());
        net.minecraft.core.particles.ParticleType<?> type = net.minecraft.core.registries.BuiltInRegistries.PARTICLE_TYPE.get(id);
        if (type == null) throw new IllegalArgumentException("Unknown particle " + particle.getKey());
        if (type instanceof net.minecraft.core.particles.SimpleParticleType simple) return simple;
        if (data instanceof org.bukkit.block.data.BlockData blockData) {
            net.minecraft.world.level.block.state.BlockState state = blockData instanceof org.bukkit.craftbukkit.block.data.CraftBlockData craft
                    ? craft.getState()
                    : ((org.bukkit.craftbukkit.block.data.CraftBlockData) org.bukkit.Bukkit.createBlockData(blockData.getAsString())).getState();
            @SuppressWarnings("unchecked") var typed=(net.minecraft.core.particles.ParticleType<net.minecraft.core.particles.BlockParticleOption>) type;
            return new net.minecraft.core.particles.BlockParticleOption(typed,state);
        }
        if (data instanceof org.bukkit.inventory.ItemStack stack) {
            @SuppressWarnings("unchecked") var typed=(net.minecraft.core.particles.ParticleType<net.minecraft.core.particles.ItemParticleOption>) type;
            return new net.minecraft.core.particles.ItemParticleOption(typed, org.bukkit.craftbukkit.inventory.CraftItemStack.asNMSCopy(stack));
        }
        if (data instanceof org.bukkit.Particle.DustOptions dust) {
            org.bukkit.Color c=dust.getColor();
            return new net.minecraft.core.particles.DustParticleOptions(
                    new org.joml.Vector3f(c.getRed()/255.0F,c.getGreen()/255.0F,c.getBlue()/255.0F),dust.getSize());
        }
        if (data instanceof org.bukkit.Particle.DustTransition transition) {
            org.bukkit.Color from=transition.getColor(); org.bukkit.Color to=transition.getToColor();
            return new net.minecraft.core.particles.DustColorTransitionOptions(
                    new org.joml.Vector3f(from.getRed()/255.0F,from.getGreen()/255.0F,from.getBlue()/255.0F),
                    new org.joml.Vector3f(to.getRed()/255.0F,to.getGreen()/255.0F,to.getBlue()/255.0F),transition.getSize());
        }
        throw new IllegalArgumentException("Particle " + particle.getKey() + " requires data of type " + particle.getDataType().getName());
    }

    public static org.bukkit.Particle minecraftToBukkit(net.minecraft.core.particles.ParticleType<?> type) {
        net.minecraft.resources.ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.PARTICLE_TYPE.getKey(type);
        if (id == null) return null;
        for (org.bukkit.Particle particle : org.bukkit.Particle.values()) {
            if (particle.getKey().getNamespace().equals(id.getNamespace()) && particle.getKey().getKey().equals(id.getPath())) return particle;
        }
        return null;
    }
}
