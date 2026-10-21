package io.lunararcdevs.lunararc.common.mixin.core.events;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.lunararcdevs.lunararc.common.server.LunarArcTagEvents;
import io.papermc.paper.registry.RegistryKey;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagLoader;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** Paper's tag lifecycle events (pre-flatten and post-flatten) around the point where a registry's raw tag entries are resolved. */
@Mixin(TagLoader.class)
public abstract class GameplayTagLoaderMixin<T> {
    @Shadow @Final private TagLoader.ElementLookup<T> elementLookup;
    @Shadow @Final private String directory;

    @WrapMethod(method = "build", require = 1)
    private Map<Identifier, List<T>> lunararc$tagEvents(Map<Identifier, List<TagLoader.EntryWithSource>> builders,
            Operation<Map<Identifier, List<T>>> original) {
        RegistryKey<?> key = LunarArcTagEvents.listenedKey(this.directory);
        if (key == null) return original.call(builders);
        Map<Identifier, List<TagLoader.EntryWithSource>> edited = LunarArcTagEvents.pre(key, builders);
        return LunarArcTagEvents.post(key, original.call(edited), this.elementLookup);
    }
}
