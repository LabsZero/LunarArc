package com.destroystokyo.paper.entity.ai;

import java.lang.reflect.Constructor;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.ai.goal.Goal;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Mob;

/** Mapping helpers for the Paper MobGoals API without modifying the loader-owned Goal class. */
public final class MobGoalHelper {
    private static final Map<Class<? extends Goal>, Class<? extends Mob>> ENTITY_CLASS_CACHE = new ConcurrentHashMap<>();
    private static final Map<Class<? extends net.minecraft.world.entity.Mob>, Class<? extends Mob>> BUKKIT_TYPES = new java.util.HashMap<>();

    static {
        put(net.minecraft.world.entity.Mob.class, org.bukkit.entity.Mob.class);
        put(net.minecraft.world.entity.PathfinderMob.class, org.bukkit.entity.Creature.class);
        put(net.minecraft.world.entity.AgeableMob.class, org.bukkit.entity.Ageable.class);
        put(net.minecraft.world.entity.FlyingMob.class, org.bukkit.entity.Flying.class);
        put(net.minecraft.world.entity.TamableAnimal.class, org.bukkit.entity.Tameable.class);
        put(net.minecraft.world.entity.ambient.AmbientCreature.class, org.bukkit.entity.Ambient.class);
        put(net.minecraft.world.entity.ambient.Bat.class, org.bukkit.entity.Bat.class);
        put(net.minecraft.world.entity.animal.Animal.class, org.bukkit.entity.Animals.class);
        put(net.minecraft.world.entity.animal.Bee.class, org.bukkit.entity.Bee.class);
        put(net.minecraft.world.entity.animal.Cat.class, org.bukkit.entity.Cat.class);
        put(net.minecraft.world.entity.animal.Chicken.class, org.bukkit.entity.Chicken.class);
        put(net.minecraft.world.entity.animal.Cod.class, org.bukkit.entity.Cod.class);
        put(net.minecraft.world.entity.animal.Cow.class, org.bukkit.entity.Cow.class);
        put(net.minecraft.world.entity.animal.Dolphin.class, org.bukkit.entity.Dolphin.class);
        put(net.minecraft.world.entity.animal.Fox.class, org.bukkit.entity.Fox.class);
        put(net.minecraft.world.entity.animal.AbstractFish.class, org.bukkit.entity.Fish.class);
        put(net.minecraft.world.entity.animal.AbstractSchoolingFish.class, io.papermc.paper.entity.SchoolableFish.class);
        put(net.minecraft.world.entity.animal.AbstractGolem.class, org.bukkit.entity.Golem.class);
        put(net.minecraft.world.entity.animal.IronGolem.class, org.bukkit.entity.IronGolem.class);
        put(net.minecraft.world.entity.animal.MushroomCow.class, org.bukkit.entity.MushroomCow.class);
        put(net.minecraft.world.entity.animal.Ocelot.class, org.bukkit.entity.Ocelot.class);
        put(net.minecraft.world.entity.animal.Panda.class, org.bukkit.entity.Panda.class);
        put(net.minecraft.world.entity.animal.Parrot.class, org.bukkit.entity.Parrot.class);
        put(net.minecraft.world.entity.animal.Pig.class, org.bukkit.entity.Pig.class);
        put(net.minecraft.world.entity.animal.PolarBear.class, org.bukkit.entity.PolarBear.class);
        put(net.minecraft.world.entity.animal.Pufferfish.class, org.bukkit.entity.PufferFish.class);
        put(net.minecraft.world.entity.animal.Rabbit.class, org.bukkit.entity.Rabbit.class);
        put(net.minecraft.world.entity.animal.Salmon.class, org.bukkit.entity.Salmon.class);
        put(net.minecraft.world.entity.animal.Sheep.class, org.bukkit.entity.Sheep.class);
        put(net.minecraft.world.entity.animal.SnowGolem.class, org.bukkit.entity.Snowman.class);
        put(net.minecraft.world.entity.animal.Squid.class, org.bukkit.entity.Squid.class);
        put(net.minecraft.world.entity.animal.TropicalFish.class, org.bukkit.entity.TropicalFish.class);
        put(net.minecraft.world.entity.animal.Turtle.class, org.bukkit.entity.Turtle.class);
        put(net.minecraft.world.entity.animal.WaterAnimal.class, org.bukkit.entity.WaterMob.class);
        put(net.minecraft.world.entity.animal.Wolf.class, org.bukkit.entity.Wolf.class);
        put(net.minecraft.world.entity.animal.axolotl.Axolotl.class, org.bukkit.entity.Axolotl.class);
        put(net.minecraft.world.entity.animal.camel.Camel.class, org.bukkit.entity.Camel.class);
        put(net.minecraft.world.entity.animal.frog.Frog.class, org.bukkit.entity.Frog.class);
        put(net.minecraft.world.entity.animal.frog.Tadpole.class, org.bukkit.entity.Tadpole.class);
        put(net.minecraft.world.entity.animal.goat.Goat.class, org.bukkit.entity.Goat.class);
        put(net.minecraft.world.entity.animal.sniffer.Sniffer.class, org.bukkit.entity.Sniffer.class);
        put(net.minecraft.world.entity.animal.armadillo.Armadillo.class, org.bukkit.entity.Armadillo.class);
        put(net.minecraft.world.entity.animal.horse.AbstractHorse.class, org.bukkit.entity.AbstractHorse.class);
        put(net.minecraft.world.entity.animal.horse.AbstractChestedHorse.class, org.bukkit.entity.ChestedHorse.class);
        put(net.minecraft.world.entity.animal.horse.Horse.class, org.bukkit.entity.Horse.class);
        put(net.minecraft.world.entity.animal.horse.Donkey.class, org.bukkit.entity.Donkey.class);
        put(net.minecraft.world.entity.animal.horse.Mule.class, org.bukkit.entity.Mule.class);
        put(net.minecraft.world.entity.animal.horse.SkeletonHorse.class, org.bukkit.entity.SkeletonHorse.class);
        put(net.minecraft.world.entity.animal.horse.ZombieHorse.class, org.bukkit.entity.ZombieHorse.class);
        put(net.minecraft.world.entity.animal.horse.Llama.class, org.bukkit.entity.Llama.class);
        put(net.minecraft.world.entity.animal.horse.TraderLlama.class, org.bukkit.entity.TraderLlama.class);
        put(net.minecraft.world.entity.monster.Monster.class, org.bukkit.entity.Monster.class);
        put(net.minecraft.world.entity.monster.Blaze.class, org.bukkit.entity.Blaze.class);
        put(net.minecraft.world.entity.monster.Bogged.class, org.bukkit.entity.Bogged.class);
        put(net.minecraft.world.entity.monster.CaveSpider.class, org.bukkit.entity.CaveSpider.class);
        put(net.minecraft.world.entity.monster.Creeper.class, org.bukkit.entity.Creeper.class);
        put(net.minecraft.world.entity.monster.Drowned.class, org.bukkit.entity.Drowned.class);
        put(net.minecraft.world.entity.monster.EnderMan.class, org.bukkit.entity.Enderman.class);
        put(net.minecraft.world.entity.monster.Endermite.class, org.bukkit.entity.Endermite.class);
        put(net.minecraft.world.entity.monster.Evoker.class, org.bukkit.entity.Evoker.class);
        put(net.minecraft.world.entity.monster.Ghast.class, org.bukkit.entity.Ghast.class);
        put(net.minecraft.world.entity.monster.Giant.class, org.bukkit.entity.Giant.class);
        put(net.minecraft.world.entity.monster.Guardian.class, org.bukkit.entity.Guardian.class);
        put(net.minecraft.world.entity.monster.ElderGuardian.class, org.bukkit.entity.ElderGuardian.class);
        put(net.minecraft.world.entity.monster.Husk.class, org.bukkit.entity.Husk.class);
        put(net.minecraft.world.entity.monster.Illusioner.class, org.bukkit.entity.Illusioner.class);
        put(net.minecraft.world.entity.monster.MagmaCube.class, org.bukkit.entity.MagmaCube.class);
        put(net.minecraft.world.entity.monster.Phantom.class, org.bukkit.entity.Phantom.class);
        put(net.minecraft.world.entity.monster.Pillager.class, org.bukkit.entity.Pillager.class);
        put(net.minecraft.world.entity.monster.Ravager.class, org.bukkit.entity.Ravager.class);
        put(net.minecraft.world.entity.monster.Shulker.class, org.bukkit.entity.Shulker.class);
        put(net.minecraft.world.entity.monster.Silverfish.class, org.bukkit.entity.Silverfish.class);
        put(net.minecraft.world.entity.monster.Skeleton.class, org.bukkit.entity.Skeleton.class);
        put(net.minecraft.world.entity.monster.AbstractSkeleton.class, org.bukkit.entity.AbstractSkeleton.class);
        put(net.minecraft.world.entity.monster.Spider.class, org.bukkit.entity.Spider.class);
        put(net.minecraft.world.entity.monster.Stray.class, org.bukkit.entity.Stray.class);
        put(net.minecraft.world.entity.monster.Strider.class, org.bukkit.entity.Strider.class);
        put(net.minecraft.world.entity.monster.Vex.class, org.bukkit.entity.Vex.class);
        put(net.minecraft.world.entity.monster.Vindicator.class, org.bukkit.entity.Vindicator.class);
        put(net.minecraft.world.entity.monster.Witch.class, org.bukkit.entity.Witch.class);
        put(net.minecraft.world.entity.monster.WitherSkeleton.class, org.bukkit.entity.WitherSkeleton.class);
        put(net.minecraft.world.entity.monster.Zoglin.class, org.bukkit.entity.Zoglin.class);
        put(net.minecraft.world.entity.monster.Zombie.class, org.bukkit.entity.Zombie.class);
        put(net.minecraft.world.entity.monster.ZombieVillager.class, org.bukkit.entity.ZombieVillager.class);
        put(net.minecraft.world.entity.monster.ZombifiedPiglin.class, org.bukkit.entity.PigZombie.class);
        put(net.minecraft.world.entity.monster.breeze.Breeze.class, org.bukkit.entity.Breeze.class);
        put(net.minecraft.world.entity.monster.hoglin.Hoglin.class, org.bukkit.entity.Hoglin.class);
        put(net.minecraft.world.entity.monster.piglin.AbstractPiglin.class, org.bukkit.entity.PiglinAbstract.class);
        put(net.minecraft.world.entity.monster.piglin.Piglin.class, org.bukkit.entity.Piglin.class);
        put(net.minecraft.world.entity.monster.piglin.PiglinBrute.class, org.bukkit.entity.PiglinBrute.class);
        put(net.minecraft.world.entity.monster.warden.Warden.class, org.bukkit.entity.Warden.class);
        put(net.minecraft.world.entity.npc.AbstractVillager.class, org.bukkit.entity.AbstractVillager.class);
        put(net.minecraft.world.entity.npc.Villager.class, org.bukkit.entity.Villager.class);
        put(net.minecraft.world.entity.npc.WanderingTrader.class, org.bukkit.entity.WanderingTrader.class);
        put(net.minecraft.world.entity.raid.Raider.class, org.bukkit.entity.Raider.class);
        put(net.minecraft.world.entity.boss.enderdragon.EnderDragon.class, org.bukkit.entity.EnderDragon.class);
        put(net.minecraft.world.entity.boss.wither.WitherBoss.class, org.bukkit.entity.Wither.class);
        put(net.minecraft.world.entity.GlowSquid.class, org.bukkit.entity.GlowSquid.class);
        put(net.minecraft.world.entity.animal.allay.Allay.class, org.bukkit.entity.Allay.class);
    }

    private MobGoalHelper() {}

    private static void put(Class<? extends net.minecraft.world.entity.Mob> nms, Class<? extends Mob> bukkit) {
        BUKKIT_TYPES.put(nms, bukkit);
    }

    public static EnumSet<GoalType> vanillaToPaper(Goal goal) {
        EnumSet<GoalType> result = EnumSet.noneOf(GoalType.class);
        var flags = goal.getFlags();
        if (flags.contains(Goal.Flag.MOVE)) result.add(GoalType.MOVE);
        if (flags.contains(Goal.Flag.LOOK)) result.add(GoalType.LOOK);
        if (flags.contains(Goal.Flag.JUMP)) result.add(GoalType.JUMP);
        if (flags.contains(Goal.Flag.TARGET)) result.add(GoalType.TARGET);
        if (flags.isEmpty()) result.add(GoalType.UNKNOWN_BEHAVIOR);
        return result;
    }

    public static EnumSet<Goal.Flag> paperToVanilla(EnumSet<GoalType> types) {
        EnumSet<Goal.Flag> result = EnumSet.noneOf(Goal.Flag.class);
        if (types.contains(GoalType.MOVE)) result.add(Goal.Flag.MOVE);
        if (types.contains(GoalType.LOOK)) result.add(Goal.Flag.LOOK);
        if (types.contains(GoalType.JUMP)) result.add(Goal.Flag.JUMP);
        if (types.contains(GoalType.TARGET)) result.add(Goal.Flag.TARGET);
        // Paper's UNKNOWN_BEHAVIOR is represented by an empty vanilla flag set in LunarArc.
        return result;
    }

    public static Goal.Flag paperToVanilla(GoalType type) {
        return switch (type) {
            case MOVE -> Goal.Flag.MOVE;
            case LOOK -> Goal.Flag.LOOK;
            case JUMP -> Goal.Flag.JUMP;
            case TARGET -> Goal.Flag.TARGET;
            case UNKNOWN_BEHAVIOR -> throw new IllegalArgumentException("UNKNOWN_BEHAVIOR has no vanilla NMS flag");
        };
    }

    public static boolean hasType(Goal goal, GoalType type) {
        if (type == GoalType.UNKNOWN_BEHAVIOR) return goal.getFlags().isEmpty();
        return goal.getFlags().contains(paperToVanilla(type));
    }

    public static <T extends Mob> GoalKey<T> getKey(Class<? extends Goal> goalClass) {
        @SuppressWarnings("unchecked")
        Class<T> entityClass = (Class<T>) ENTITY_CLASS_CACHE.computeIfAbsent(goalClass, MobGoalHelper::resolveEntityClass);
        return GoalKey.of(entityClass, NamespacedKey.minecraft(getUsableName(goalClass)));
    }

    private static Class<? extends Mob> resolveEntityClass(Class<? extends Goal> goalClass) {
        for (Constructor<?> constructor : goalClass.getDeclaredConstructors()) {
            for (Class<?> parameter : constructor.getParameterTypes()) {
                if (net.minecraft.world.entity.Mob.class.isAssignableFrom(parameter)) {
                    @SuppressWarnings("unchecked")
                    Class<? extends net.minecraft.world.entity.Mob> nms = (Class<? extends net.minecraft.world.entity.Mob>) parameter;
                    return toBukkitClass(nms);
                }
                if (net.minecraft.world.entity.monster.RangedAttackMob.class.isAssignableFrom(parameter)) {
                    return com.destroystokyo.paper.entity.RangedEntity.class;
                }
            }
        }
        return Mob.class;
    }

    public static Class<? extends Mob> toBukkitClass(Class<? extends net.minecraft.world.entity.Mob> nmsClass) {
        Class<? extends Mob> exact = BUKKIT_TYPES.get(nmsClass);
        if (exact != null) return exact;
        Class<? extends Mob> best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (Map.Entry<Class<? extends net.minecraft.world.entity.Mob>, Class<? extends Mob>> entry : BUKKIT_TYPES.entrySet()) {
            if (!entry.getKey().isAssignableFrom(nmsClass)) continue;
            int distance = inheritanceDistance(nmsClass, entry.getKey());
            if (distance < bestDistance) {
                best = entry.getValue();
                bestDistance = distance;
            }
        }
        return best != null ? best : Mob.class;
    }

    private static int inheritanceDistance(Class<?> child, Class<?> ancestor) {
        int distance = 0;
        for (Class<?> current = child; current != null; current = current.getSuperclass(), distance++) {
            if (current == ancestor) return distance;
        }
        return Integer.MAX_VALUE - 1;
    }

    public static String getUsableName(Class<?> goalClass) {
        String name = goalClass.getSimpleName();
        if (name == null || name.isBlank()) name = goalClass.getName().substring(goalClass.getName().lastIndexOf('.') + 1);
        int dollar = name.lastIndexOf('$');
        if (dollar >= 0 && dollar + 1 < name.length()) name = name.substring(dollar + 1);
        name = name.replace("PathfinderGoal", "").replace("TargetGoal", "").replace("Goal", "");
        StringBuilder key = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c) && key.length() > 0) key.append('_');
            if (Character.isLetterOrDigit(c) || c == '_' || c == '-') key.append(Character.toLowerCase(c));
        }
        if (key.length() == 0) key.append("unknown");
        return key.toString();
    }
}
