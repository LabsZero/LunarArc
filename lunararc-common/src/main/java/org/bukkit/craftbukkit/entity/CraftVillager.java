package org.bukkit.craftbukkit.entity;

import com.destroystokyo.paper.entity.villager.Reputation;
import com.destroystokyo.paper.entity.villager.ReputationType;
import io.lunararcdevs.lunararc.common.bridge.entity.VillagerBridge;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.gossip.GossipContainer;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.item.trading.VillagerTrades;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Villager;
import org.bukkit.entity.ZombieVillager;

public final class CraftVillager extends CraftAbstractVillager implements Villager {
    public CraftVillager(CraftServer server, net.minecraft.world.entity.npc.villager.Villager entity) { super(server, entity); }
    @Override public net.minecraft.world.entity.npc.villager.Villager getHandle() { return (net.minecraft.world.entity.npc.villager.Villager) this.entity; }
    private VillagerBridge concreteVillagerBridge() { return (VillagerBridge) (Object) getHandle(); }

    @Override
    public Profession getProfession() {
        Identifier key = getHandle().getVillagerData().profession().unwrapKey()
                .orElseThrow(() -> new IllegalStateException("Villager profession is not registry-backed")).identifier();
        Profession profession = Registry.VILLAGER_PROFESSION.get(new NamespacedKey(key.getNamespace(), key.getPath()));
        if (profession == null) throw new IllegalStateException("No Bukkit profession for " + key);
        return profession;
    }

    @Override
    public void setProfession(Profession profession) {
        Objects.requireNonNull(profession, "profession");
        NamespacedKey key = profession.getKey();
        net.minecraft.core.Holder<net.minecraft.world.entity.npc.villager.VillagerProfession> nms = server.getServer().registryAccess().lookupOrThrow(Registries.VILLAGER_PROFESSION)
                .get(net.minecraft.resources.ResourceKey.create(Registries.VILLAGER_PROFESSION, Identifier.fromNamespaceAndPath(key.getNamespace(), key.getKey())))
                .orElseThrow(() -> new IllegalArgumentException("Unknown villager profession " + key));
        getHandle().setVillagerData(getHandle().getVillagerData().withProfession(nms));
    }

    @Override
    public Type getVillagerType() {
        Identifier key = getHandle().getVillagerData().type().unwrapKey()
                .orElseThrow(() -> new IllegalStateException("Villager type is not registry-backed")).identifier();
        Type type = Registry.VILLAGER_TYPE.get(new NamespacedKey(key.getNamespace(), key.getPath()));
        if (type == null) throw new IllegalStateException("No Bukkit villager type for " + key);
        return type;
    }

    @Override
    public void setVillagerType(Type type) {
        Objects.requireNonNull(type, "type");
        NamespacedKey key = type.getKey();
        net.minecraft.core.Holder<net.minecraft.world.entity.npc.villager.VillagerType> nms = server.getServer().registryAccess().lookupOrThrow(Registries.VILLAGER_TYPE)
                .get(net.minecraft.resources.ResourceKey.create(Registries.VILLAGER_TYPE, Identifier.fromNamespaceAndPath(key.getNamespace(), key.getKey())))
                .orElseThrow(() -> new IllegalArgumentException("Unknown villager type " + key));
        getHandle().setVillagerData(getHandle().getVillagerData().withType(nms));
    }

    @Override public int getVillagerLevel() { return getHandle().getVillagerData().level(); }
    @Override public void setVillagerLevel(int level) {
        if (level < 1 || level > 5) throw new IllegalArgumentException("level must be between 1 and 5");
        getHandle().setVillagerData(getHandle().getVillagerData().withLevel(level));
    }
    @Override public int getVillagerExperience() { return concreteVillagerBridge().lunararc$getVillagerXp(); }
    @Override public void setVillagerExperience(int experience) {
        if (experience < 0) throw new IllegalArgumentException("experience must be >= 0");
        concreteVillagerBridge().lunararc$setVillagerXp(experience);
    }

    @Override
    public boolean increaseLevel(int amount) {
        if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
        int target = getVillagerLevel() + amount;
        if (target > 5) throw new IllegalArgumentException("final villager level must be <= 5");
        if (!(getHandle().level() instanceof net.minecraft.server.level.ServerLevel level)) return false;
        boolean added = false;
        while (getVillagerLevel() < target) {
            int before = getHandle().getOffers().size();
            concreteVillagerBridge().lunararc$increaseMerchantCareer(level);
            added |= getHandle().getOffers().size() > before;
        }
        return added;
    }

    @Override
    public boolean addTrades(int amount) {
        if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
        var profession = getHandle().getVillagerData().profession();
        net.minecraft.resources.ResourceKey<net.minecraft.world.item.trading.TradeSet> tradeSet =
                profession.value().tradeSetsByLevel().get(getVillagerLevel());
        if (tradeSet == null) return false;
        net.minecraft.world.item.trading.MerchantOffers offers = getHandle().getOffers();
        int before = offers.size();
        for (int i = 0; i < amount; i++) {
            int size = offers.size();
            super.villagerBridge().lunararc$addOffersFromTradeSet(offers, tradeSet);
            if (offers.size() == size) break;
        }
        return offers.size() > before;
    }

    @Override public int getRestocksToday() { return concreteVillagerBridge().lunararc$getRestocksToday(); }
    @Override public void setRestocksToday(int restocksToday) { concreteVillagerBridge().lunararc$setRestocksToday(restocksToday); }

    @Override
    public boolean sleep(Location location) {
        Objects.requireNonNull(location, "location");
        if (location.getWorld() == null || !location.getWorld().equals(getWorld())) throw new IllegalArgumentException("location must be in the villager's world");
        BlockPos pos = BlockPos.containing(location.getX(), location.getY(), location.getZ());
        if (!(getHandle().level().getBlockState(pos).getBlock() instanceof net.minecraft.world.level.block.BedBlock)) return false;
        getHandle().startSleeping(pos);
        return true;
    }
    @Override public void wakeup() {
        if (!isSleeping()) throw new IllegalStateException("villager is not sleeping");
        getHandle().stopSleeping();
    }
    @Override public void shakeHead() { getHandle().setUnhappy(); }

    @Override
    public ZombieVillager zombify() {
        if (!(getHandle().level() instanceof net.minecraft.server.level.ServerLevel level)) return null;
        net.minecraft.world.entity.monster.zombie.ZombieVillager converted =
                io.lunararcdevs.lunararc.common.mod.util.LunarArcEntityTransforms.convert(
                        getHandle(),
                        net.minecraft.world.entity.EntityTypes.ZOMBIE_VILLAGER,
                        false,
                        org.bukkit.event.entity.EntityTransformEvent.TransformReason.INFECTION,
                        org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.CUSTOM,
                        zombie -> {
                            zombie.setVillagerData(getHandle().getVillagerData());
                            zombie.setGossips(getHandle().getGossips().copy());
                            zombie.setTradeOffers(getHandle().getOffers().copy());
                            zombie.setVillagerXp(getHandle().getVillagerXp());
                        });
        if (converted == null) return null;
        converted.finalizeSpawn(
                level,
                level.getCurrentDifficultyAt(converted.blockPosition()),
                net.minecraft.world.entity.EntitySpawnReason.CONVERSION,
                new net.minecraft.world.entity.monster.zombie.Zombie.ZombieGroupData(false, true));
        return (ZombieVillager) ((io.lunararcdevs.lunararc.common.bridge.EntityBridge) converted).lunararc$getBukkitEntity();
    }

    @Override
    public Reputation getReputation(UUID uniqueId) {
        Objects.requireNonNull(uniqueId, "uniqueId");
        EnumMap<ReputationType, Integer> values = new EnumMap<>(ReputationType.class);
        var entries = getHandle().getGossips().getGossipEntries().get(uniqueId);
        if (entries != null) {
            for (ReputationType type : ReputationType.values()) {
                int value = entries.getOrDefault(GossipType.valueOf(type.name()), 0);
                if (value != 0) values.put(type, value);
            }
        }
        return new Reputation(values);
    }

    @Override
    public Map<UUID, Reputation> getReputations() {
        Map<UUID, Reputation> result = new HashMap<>();
        for (UUID id : getHandle().getGossips().getGossipEntries().keySet()) result.put(id, getReputation(id));
        return result;
    }

    @Override
    public void setReputation(UUID uniqueId, Reputation reputation) {
        Objects.requireNonNull(uniqueId, "uniqueId");
        Objects.requireNonNull(reputation, "reputation");
        GossipContainer gossips = getHandle().getGossips();
        for (ReputationType type : ReputationType.values()) {
            GossipType nms = GossipType.valueOf(type.name());
            gossips.remove(uniqueId, nms);
            if (reputation.hasReputationSet(type)) {
                int value = reputation.getReputation(type);
                if (value != 0) gossips.add(uniqueId, nms, value);
            }
        }
    }

    @Override public void setReputations(Map<UUID, Reputation> reputations) {
        Objects.requireNonNull(reputations, "reputations");
        for (Map.Entry<UUID, Reputation> entry : reputations.entrySet()) setReputation(entry.getKey(), entry.getValue());
    }

    @Override
    public void clearReputations() {
        GossipContainer gossips = getHandle().getGossips();
        for (UUID id : java.util.List.copyOf(gossips.getGossipEntries().keySet())) {
            for (GossipType type : GossipType.values()) gossips.remove(id, type);
        }
    }

    @Override public String toString() { return "CraftVillager"; }

    @Override
    public void restock() {
        getHandle().restock();
    }

    @Override
    public void updateDemand() {
        getHandle().updateDemand();
    }

    public static class CraftType extends io.papermc.paper.util.OldEnumHolderable<Type, net.minecraft.world.entity.npc.villager.VillagerType> implements Type {
        private static int count = 0;

        public static Type minecraftHolderToBukkit(net.minecraft.core.Holder<net.minecraft.world.entity.npc.villager.VillagerType> minecraft) {
            return org.bukkit.craftbukkit.CraftRegistry.minecraftHolderToBukkit(minecraft, net.minecraft.core.registries.Registries.VILLAGER_TYPE);
        }

        public static net.minecraft.core.Holder<net.minecraft.world.entity.npc.villager.VillagerType> bukkitToMinecraftHolder(Type bukkit) {
            return org.bukkit.craftbukkit.CraftRegistry.bukkitToMinecraftHolder(bukkit);
        }

        public CraftType(final net.minecraft.core.Holder<net.minecraft.world.entity.npc.villager.VillagerType> holder) {
            super(holder, count++);
        }
    }

    public static class CraftProfession extends io.papermc.paper.util.OldEnumHolderable<Profession, net.minecraft.world.entity.npc.villager.VillagerProfession> implements Profession {
        private static int count = 0;

        public static Profession minecraftHolderToBukkit(net.minecraft.core.Holder<net.minecraft.world.entity.npc.villager.VillagerProfession> minecraft) {
            return org.bukkit.craftbukkit.CraftRegistry.minecraftHolderToBukkit(minecraft, net.minecraft.core.registries.Registries.VILLAGER_PROFESSION);
        }

        public static net.minecraft.core.Holder<net.minecraft.world.entity.npc.villager.VillagerProfession> bukkitToMinecraftHolder(Profession bukkit) {
            return org.bukkit.craftbukkit.CraftRegistry.bukkitToMinecraftHolder(bukkit);
        }

        public CraftProfession(final net.minecraft.core.Holder<net.minecraft.world.entity.npc.villager.VillagerProfession> holder) {
            super(holder, count++);
        }
    }
}
