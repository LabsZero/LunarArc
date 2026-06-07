package io.ampznetwork.lunararc.common.mixin.bukkit;

import org.bukkit.NamespacedKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Locale;

@Mixin(targets = "org.bukkit.Material")
public abstract class MaterialMixin {

    @Shadow(remap = false)
    public abstract boolean isLegacy();

    @Inject(method = "getKey", at = @At("HEAD"), cancellable = true, remap = false)
    private void lunararc$onGetKey(CallbackInfoReturnable<NamespacedKey> cir) {
        if (isLegacy()) {
            cir.setReturnValue(NamespacedKey.minecraft("air"));
        }
    }

    @Inject(method = "isItem", at = @At("HEAD"), cancellable = true, remap = false)
    private void lunararc$isItem(CallbackInfoReturnable<Boolean> cir) {
        if (isLegacy()) {
            cir.setReturnValue(false);
            return;
        }
        try {
            String path = ((Enum<?>) (Object) this).name().toLowerCase(Locale.ROOT);
            net.minecraft.resources.ResourceLocation rl =
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft", path);
            cir.setReturnValue(net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(rl));
        } catch (Exception ignored) {}
    }

    @Inject(method = "isBlock", at = @At("HEAD"), cancellable = true, remap = false)
    private void lunararc$isBlock(CallbackInfoReturnable<Boolean> cir) {
        if (isLegacy()) {
            cir.setReturnValue(false);
            return;
        }
        try {
            String path = ((Enum<?>) (Object) this).name().toLowerCase(Locale.ROOT);
            net.minecraft.resources.ResourceLocation rl =
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft", path);
            cir.setReturnValue(net.minecraft.core.registries.BuiltInRegistries.BLOCK.containsKey(rl));
        } catch (Exception ignored) {}
    }
}
