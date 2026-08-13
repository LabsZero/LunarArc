package org.bukkit.craftbukkit.v1_21_R1.inventory.components;

import com.google.common.base.Preconditions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v1_21_R1.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.components.FoodComponent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Paper 1.21.1 snapshot wrapper for the FOOD data component. */
public final class CraftFoodComponent implements FoodComponent {
    private FoodProperties handle;

    public CraftFoodComponent(FoodProperties handle) {
        this.handle = Objects.requireNonNull(handle, "handle");
    }

    public CraftFoodComponent(FoodComponent source) {
        Objects.requireNonNull(source, "source");
        ItemStack remainder = source.getUsingConvertsTo();
        this.handle = new FoodProperties(
                source.getNutrition(),
                source.getSaturation(),
                source.canAlwaysEat(),
                source.getEatSeconds(),
                Optional.ofNullable(remainder).map(CraftItemStack::asNMSCopy),
                source.getEffects().stream().map(CraftFoodEffect::new).map(CraftFoodEffect::getHandle).toList());
    }

    public FoodProperties getHandle() { return this.handle; }

    @Override public int getNutrition() { return handle.nutrition(); }
    @Override public void setNutrition(int nutrition) {
        Preconditions.checkArgument(nutrition >= 0, "Nutrition cannot be negative");
        handle = new FoodProperties(nutrition, handle.saturation(), handle.canAlwaysEat(), handle.eatSeconds(), handle.usingConvertsTo(), handle.effects());
    }
    @Override public float getSaturation() { return handle.saturation(); }
    @Override public void setSaturation(float saturation) {
        handle = new FoodProperties(handle.nutrition(), saturation, handle.canAlwaysEat(), handle.eatSeconds(), handle.usingConvertsTo(), handle.effects());
    }
    @Override public boolean canAlwaysEat() { return handle.canAlwaysEat(); }
    @Override public void setCanAlwaysEat(boolean value) {
        handle = new FoodProperties(handle.nutrition(), handle.saturation(), value, handle.eatSeconds(), handle.usingConvertsTo(), handle.effects());
    }
    @Override public float getEatSeconds() { return handle.eatSeconds(); }
    @Override public void setEatSeconds(float seconds) {
        Preconditions.checkArgument(seconds > 0, "Eat seconds must be positive");
        handle = new FoodProperties(handle.nutrition(), handle.saturation(), handle.canAlwaysEat(), seconds, handle.usingConvertsTo(), handle.effects());
    }
    @Override public ItemStack getUsingConvertsTo() {
        return handle.usingConvertsTo().map(CraftItemStack::asBukkitCopy).orElse(null);
    }
    @Override public void setUsingConvertsTo(ItemStack item) {
        Preconditions.checkArgument(item == null || !item.isEmpty(), "Item cannot be empty");
        handle = new FoodProperties(handle.nutrition(), handle.saturation(), handle.canAlwaysEat(), handle.eatSeconds(), Optional.ofNullable(item).map(CraftItemStack::asNMSCopy), handle.effects());
    }
    @Override public List<FoodEffect> getEffects() {
        return handle.effects().stream().map(CraftFoodEffect::new).map(e -> (FoodEffect) e).toList();
    }
    @Override public void setEffects(List<FoodEffect> effects) {
        Objects.requireNonNull(effects, "effects");
        handle = new FoodProperties(handle.nutrition(), handle.saturation(), handle.canAlwaysEat(), handle.eatSeconds(), handle.usingConvertsTo(), effects.stream().map(CraftFoodEffect::new).map(CraftFoodEffect::getHandle).toList());
    }
    @Override public FoodEffect addEffect(PotionEffect effect, float probability) {
        Preconditions.checkArgument(effect != null, "effect cannot be null");
        Preconditions.checkArgument(probability >= 0.0f && probability <= 1.0f, "Probability cannot be outside range [0,1]");
        CraftFoodEffect added = new CraftFoodEffect(effect, probability);
        List<FoodProperties.PossibleEffect> effects = new ArrayList<>(handle.effects());
        effects.add(added.getHandle());
        handle = new FoodProperties(handle.nutrition(), handle.saturation(), handle.canAlwaysEat(), handle.eatSeconds(), handle.usingConvertsTo(), effects);
        return added;
    }

    @Override public Map<String, Object> serialize() {
        Map<String,Object> out = new LinkedHashMap<>();
        out.put("nutrition", getNutrition()); out.put("saturation", getSaturation()); out.put("can-always-eat", canAlwaysEat());
        out.put("eat-seconds", getEatSeconds());
        ItemStack remainder = getUsingConvertsTo(); if (remainder != null) out.put("using-converts-to", remainder);
        out.put("effects", getEffects()); return out;
    }

    @Override public boolean equals(Object o) { return o instanceof CraftFoodComponent c && Objects.equals(handle, c.handle); }
    @Override public int hashCode() { return Objects.hashCode(handle); }
    @Override public String toString() { return "CraftFoodComponent{" + handle + '}'; }

    public static final class CraftFoodEffect implements FoodEffect {
        private FoodProperties.PossibleEffect handle;
        public CraftFoodEffect(FoodProperties.PossibleEffect handle) { this.handle = Objects.requireNonNull(handle, "handle"); }
        public CraftFoodEffect(FoodEffect source) { this(source.getEffect(), source.getProbability()); }
        public CraftFoodEffect(PotionEffect effect, float probability) { this.handle = new FoodProperties.PossibleEffect(toMinecraft(effect), probability); }
        FoodProperties.PossibleEffect getHandle() { return handle; }
        @Override public PotionEffect getEffect() { return toBukkit(handle.effect()); }
        @Override public void setEffect(PotionEffect effect) { handle = new FoodProperties.PossibleEffect(toMinecraft(effect), handle.probability()); }
        @Override public float getProbability() { return handle.probability(); }
        @Override public void setProbability(float probability) {
            Preconditions.checkArgument(probability >= 0.0f && probability <= 1.0f, "Probability cannot be outside range [0,1]");
            handle = new FoodProperties.PossibleEffect(handle.effect(), probability);
        }
        @Override public Map<String,Object> serialize() { Map<String,Object> m=new LinkedHashMap<>(); m.put("effect",getEffect()); m.put("probability",getProbability()); return m; }
        @Override public boolean equals(Object o) { return o instanceof CraftFoodEffect e && Objects.equals(handle,e.handle); }
        @Override public int hashCode() { return Objects.hashCode(handle); }
    }

    private static MobEffectInstance toMinecraft(PotionEffect effect) {
        Objects.requireNonNull(effect, "effect");
        NamespacedKey key = effect.getType().getKey();
        var holder = BuiltInRegistries.MOB_EFFECT.getHolder(net.minecraft.resources.ResourceLocation.parse(key.toString()))
                .orElseThrow(() -> new IllegalArgumentException("Unknown potion effect " + key));
        return new MobEffectInstance(holder, effect.getDuration(), effect.getAmplifier(), effect.isAmbient(), effect.hasParticles(), effect.hasIcon());
    }

    private static PotionEffect toBukkit(MobEffectInstance effect) {
        var id = BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value());
        PotionEffectType type = PotionEffectType.getByKey(NamespacedKey.fromString(id.toString()));
        if (type == null) throw new IllegalStateException("No Bukkit potion effect for " + id);
        return new PotionEffect(type, effect.getDuration(), effect.getAmplifier(), effect.isAmbient(), effect.isVisible(), effect.showIcon());
    }
}
