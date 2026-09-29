package io.lunararcdevs.lunararc.common.mixin.core.entity;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

@Mixin(AttributeMap.class)
public abstract class AttributeMapMixin {
    @Shadow @Final private Map<Holder<Attribute>, AttributeInstance> attributes;
    @Shadow private void onAttributeModified(AttributeInstance instance) { throw new AssertionError(); }

    public void registerAttribute(Holder<Attribute> attribute) {
        this.attributes.put(attribute, new AttributeInstance(attribute, this::onAttributeModified));
    }
}
