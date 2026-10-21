package io.lunararcdevs.lunararc.common.mixin.core.server;

import com.google.common.collect.ImmutableMap;
import io.lunararcdevs.lunararc.common.server.LunarArcDatapackDiscovery;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PackRepository.class)
public abstract class PackRepositoryPluginPacksMixin {
    @Inject(method = "discoverAvailable", at = @At("RETURN"), cancellable = true, require = 0)
    private void lunararc$pluginPacks(CallbackInfoReturnable<Map<String, Pack>> cir) {
        var extra = LunarArcDatapackDiscovery.packs();
        if (extra.isEmpty()) return;
        Map<String, Pack> merged = new LinkedHashMap<>(cir.getReturnValue());
        for (Pack pack : extra) merged.put(pack.getId(), pack);
        cir.setReturnValue(ImmutableMap.copyOf(merged));
    }
}
