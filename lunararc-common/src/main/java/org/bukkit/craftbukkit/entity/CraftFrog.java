package org.bukkit.craftbukkit.entity;
import net.minecraft.core.registries.Registries;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.craftbukkit.CraftServer;
public final class CraftFrog extends CraftAnimals implements org.bukkit.entity.Frog {
    public CraftFrog(CraftServer server, net.minecraft.world.entity.animal.frog.Frog entity) { super(server, entity); }
    @Override public net.minecraft.world.entity.animal.frog.Frog getHandle() { return (net.minecraft.world.entity.animal.frog.Frog) entity; }
    @Override public org.bukkit.entity.Entity getTongueTarget() { return getHandle().getTongueTarget().map(e -> CraftEntity.getEntity(server, e)).orElse(null); }
    @Override public void setTongueTarget(org.bukkit.entity.Entity target) {
        if (target == null) getHandle().eraseTongueTarget(); else getHandle().setTongueTarget(((CraftEntity)target).getHandle());
    }
    @Override public Variant getVariant() {
        var key = getHandle().getVariant().unwrapKey().orElseThrow().location();
        return Registry.FROG_VARIANT.get(new NamespacedKey(key.getNamespace(), key.getPath()));
    }
    @Override public void setVariant(Variant variant) {
        java.util.Objects.requireNonNull(variant, "variant");
        var reg = getHandle().registryAccess().registryOrThrow(Registries.FROG_VARIANT);
        var key = net.minecraft.resources.ResourceKey.create(Registries.FROG_VARIANT, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(variant.getKey().getNamespace(), variant.getKey().getKey()));
        getHandle().setVariant(reg.getHolderOrThrow(key));
    }
    @Override public String toString() { return "CraftFrog"; }
}
