package org.bukkit.craftbukkit.inventory;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import org.bukkit.DyeColor;
import org.bukkit.NamespacedKey;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.inventory.meta.BannerMeta;

/** Concrete banner metadata over Minecraft 1.21.1 BANNER_PATTERNS. */
public final class CraftMetaBanner extends CraftItemMeta implements BannerMeta {
    public static final int ARBITRARY_LIMIT = 20;
    private List<Pattern> patterns = new ArrayList<>();

    public CraftMetaBanner() { super(); }
    public CraftMetaBanner(ItemStack nms) {
        super(nms);
        if (nms == null || nms.isEmpty()) return;
        BannerPatternLayers layers = nms.get(DataComponents.BANNER_PATTERNS);
        if (layers == null) return;
        for (int i=0;i<Math.min(layers.layers().size(), ARBITRARY_LIMIT);i++) {
            BannerPatternLayers.Layer layer = layers.layers().get(i);
            DyeColor color = DyeColor.getByWoolData((byte) layer.color().getId());
            PatternType type = toBukkit(layer.pattern());
            if (color != null && type != null) this.patterns.add(new Pattern(color, type));
        }
    }

    @Override public void applyToNms(ItemStack nms) {
        super.applyToNms(nms);
        if (this.patterns.isEmpty()) { nms.remove(DataComponents.BANNER_PATTERNS); return; }
        List<BannerPatternLayers.Layer> layers = new ArrayList<>();
        for (Pattern p : this.patterns) {
            layers.add(new BannerPatternLayers.Layer(toNms(p.getPattern()), net.minecraft.world.item.DyeColor.byId(p.getColor().getWoolData())));
        }
        nms.set(DataComponents.BANNER_PATTERNS, new BannerPatternLayers(layers));
    }

    private static PatternType toBukkit(Holder<BannerPattern> holder) {
        ResourceLocation id = holder.unwrapKey().orElse(null) == null ? null : holder.unwrapKey().orElseThrow().location();
        if (id == null) return null;
        return org.bukkit.Registry.BANNER_PATTERN.get(new NamespacedKey(id.getNamespace(), id.getPath()));
    }
    private static Holder<BannerPattern> toNms(PatternType type) {
        NamespacedKey key = type.getKey();
        Registry<BannerPattern> registry = ((org.bukkit.craftbukkit.CraftServer) org.bukkit.Bukkit.getServer()).getServer().registryAccess().registryOrThrow(Registries.BANNER_PATTERN);
        ResourceKey<BannerPattern> resourceKey = ResourceKey.create(Registries.BANNER_PATTERN, ResourceLocation.fromNamespaceAndPath(key.getNamespace(), key.getKey()));
        return registry.getHolder(resourceKey).orElseThrow(() -> new IllegalArgumentException("Unknown banner pattern " + key));
    }

    @Override public List<Pattern> getPatterns() { return new ArrayList<>(this.patterns); }
    @Override public void setPatterns(List<Pattern> patterns) { this.patterns = new ArrayList<>(patterns); }
    @Override public void addPattern(Pattern pattern) { this.patterns.add(pattern); }
    @Override public Pattern getPattern(int i) { return this.patterns.get(i); }
    @Override public Pattern removePattern(int i) { return this.patterns.remove(i); }
    @Override public void setPattern(int i, Pattern pattern) { this.patterns.set(i, pattern); }
    @Override public int numberOfPatterns() { return this.patterns.size(); }
    @Override public CraftMetaBanner clone() { CraftMetaBanner c=(CraftMetaBanner)super.clone(); c.patterns=new ArrayList<>(this.patterns); return c; }
}
