package io.lunararcdevs.lunararc.common.mixin.core.server;

import io.lunararcdevs.lunararc.common.event.LunarArcMoreEvents;
import net.minecraft.server.players.StoredUserEntry;
import net.minecraft.server.players.StoredUserList;
import net.minecraft.server.players.UserWhiteList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(StoredUserList.class)
public abstract class WhitelistEventsMixin {

    @Inject(method = "add(Lnet/minecraft/server/players/StoredUserEntry;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void lunararc$add(StoredUserEntry<?> entry, CallbackInfo ci) {
        if ((Object) this instanceof UserWhiteList && entry.getUser() instanceof com.mojang.authlib.GameProfile profile
                && !LunarArcMoreEvents.whitelistUpdate(profile, true)) ci.cancel();
    }

    @Inject(method = "remove(Ljava/lang/Object;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void lunararc$remove(Object user, CallbackInfo ci) {
        if ((Object) this instanceof UserWhiteList && user instanceof com.mojang.authlib.GameProfile profile
                && !LunarArcMoreEvents.whitelistUpdate(profile, false)) ci.cancel();
    }
}
