package io.lunararcdevs.lunararc.common.mixin.core.entity;

import io.lunararcdevs.lunararc.common.bridge.entity.PiglinBridge;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;

import java.util.HashSet;
import java.util.Set;

@Mixin(Piglin.class)
public abstract class PiglinMixin implements PiglinBridge {
    public Set<Item> allowedBarterItems = new HashSet<>();
    public Set<Item> interestItems = new HashSet<>();

    @Override public Set<Item> lunararc$getAllowedBarterItems() { return this.allowedBarterItems; }
    @Override public Set<Item> lunararc$getInterestItems() { return this.interestItems; }
}
