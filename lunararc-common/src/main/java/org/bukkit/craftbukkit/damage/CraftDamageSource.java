package org.bukkit.craftbukkit.damage;

import io.lunararcdevs.lunararc.common.bridge.EntityBridge;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class CraftDamageSource implements DamageSource {
    private final net.minecraft.world.damagesource.DamageSource handle;
    private final DamageType damageType;
    private final net.kyori.adventure.pointer.Pointers damageContext;

    public CraftDamageSource(@NotNull net.minecraft.world.damagesource.DamageSource handle) {
        this(handle, net.kyori.adventure.pointer.Pointers.empty());
    }

    public CraftDamageSource(@NotNull net.minecraft.world.damagesource.DamageSource handle, @NotNull net.kyori.adventure.pointer.Pointers damageContext) {
        this.handle = Objects.requireNonNull(handle, "handle");
        this.damageContext = Objects.requireNonNull(damageContext, "damageContext");
        Identifier location = handle.typeHolder().unwrapKey()
                .map(ResourceKey::identifier)
                .orElseThrow(() -> new IllegalStateException("DamageSource type is not registered"));
        this.damageType = new CraftDamageType(
                new org.bukkit.NamespacedKey(location.getNamespace(), location.getPath()),
                handle.type());
    }

    public @NotNull net.minecraft.world.damagesource.DamageSource getHandle() {
        return this.handle;
    }

    @Override
    public @NotNull DamageType getDamageType() {
        return this.damageType;
    }

    @Override
    public @Nullable Entity getCausingEntity() {
        return bukkitEntity(this.handle.getEntity());
    }

    @Override
    public @Nullable Entity getDirectEntity() {
        return bukkitEntity(this.handle.getDirectEntity());
    }

    @Override
    public @Nullable Location getDamageLocation() {
        Vec3 position = this.handle.sourcePositionRaw();
        return position == null ? null : toBukkitLocation(position);
    }

    @Override
    public @Nullable Location getSourceLocation() {
        Vec3 position = this.handle.getSourcePosition();
        return position == null ? null : toBukkitLocation(position);
    }

    @Override
    public boolean isIndirect() {
        return !this.handle.isDirect();
    }

    @Override
    public float getFoodExhaustion() {
        return this.damageType.getExhaustion();
    }

    @Override
    public net.kyori.adventure.pointer.@NotNull Pointers getDamageContext() {
        return this.damageContext;
    }

    @Override
    public boolean scalesWithDifficulty() {
        return this.handle.scalesWithDifficulty();
    }

    private @Nullable Location toBukkitLocation(Vec3 position) {
        Entity entity = getCausingEntity();
        if (entity == null) entity = getDirectEntity();
        World world = entity == null ? null : entity.getWorld();
        return new Location(world, position.x, position.y, position.z);
    }

    private static @Nullable Entity bukkitEntity(@Nullable net.minecraft.world.entity.Entity entity) {
        if (entity == null) return null;
        return ((EntityBridge) (Object) entity).lunararc$getBukkitEntity();
    }

    static @NotNull CraftDamageSource fromBukkit(@NotNull DamageType type,
                                                  @Nullable Entity causingEntity,
                                                  @Nullable Entity directEntity,
                                                  @Nullable Location damageLocation) {
        return fromBukkit(type, causingEntity, directEntity, damageLocation, net.kyori.adventure.pointer.Pointers.empty());
    }

    static @NotNull CraftDamageSource fromBukkit(@NotNull DamageType type,
                                                  @Nullable Entity causingEntity,
                                                  @Nullable Entity directEntity,
                                                  @Nullable Location damageLocation,
                                                  @NotNull net.kyori.adventure.pointer.Pointers damageContext) {
        CraftServer server = (CraftServer) Bukkit.getServer();
        if (server == null) throw new IllegalStateException("CraftServer is not initialized");

        Identifier id = Identifier.fromNamespaceAndPath(type.getKey().getNamespace(), type.getKey().getKey());
        ResourceKey<net.minecraft.world.damagesource.DamageType> key = ResourceKey.create(Registries.DAMAGE_TYPE, id);
        Holder<net.minecraft.world.damagesource.DamageType> holder = server.getServer().registryAccess()
                .lookupOrThrow(Registries.DAMAGE_TYPE)
                .get(key)
                .orElseThrow(() -> new IllegalArgumentException("Unregistered damage type " + type.getKey()));

        net.minecraft.world.entity.Entity nmsCause = unwrap(causingEntity);
        net.minecraft.world.entity.Entity nmsDirect = unwrap(directEntity);
        Vec3 position = damageLocation == null
                ? null
                : new Vec3(damageLocation.getX(), damageLocation.getY(), damageLocation.getZ());
        return new CraftDamageSource(
                new net.minecraft.world.damagesource.DamageSource(holder, nmsDirect, nmsCause, position), damageContext);
    }

    private static @Nullable net.minecraft.world.entity.Entity unwrap(@Nullable Entity entity) {
        if (entity == null) return null;
        if (entity instanceof CraftEntity craft) return craft.getHandle();
        throw new IllegalArgumentException("DamageSource entities must be concrete CraftEntity instances: " + entity.getClass().getName());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof DamageSource other)) return false;
        return Objects.equals(this.getDamageType(), other.getDamageType())
                && Objects.equals(this.getCausingEntity(), other.getCausingEntity())
                && Objects.equals(this.getDirectEntity(), other.getDirectEntity())
                && Objects.equals(this.getDamageLocation(), other.getDamageLocation());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getDamageType(), this.getCausingEntity(), this.getDirectEntity(), this.getDamageLocation());
    }

    @Override
    public String toString() {
        return "CraftDamageSource{" + this.handle + "}";
    }
}
