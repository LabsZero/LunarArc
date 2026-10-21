package io.lunararcdevs.lunararc.common.mixin.core.server;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.SessionService;
import com.mojang.authlib.services.MinecraftServicesDiscoveryService;
import io.lunararcdevs.lunararc.common.server.LunarArcProfileServices;
import net.minecraft.server.Services;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Profile lookup and fill events: the services the server talks to Mojang through are wrapped as they are created. */
@Mixin(Services.class)
public abstract class ServerSideProfileServicesMixin {
    @WrapOperation(method = "create", require = 1,
            at = @At(value = "INVOKE", target = "Lcom/mojang/authlib/services/MinecraftServicesDiscoveryService;createProfileRepository()Lcom/mojang/authlib/GameProfileRepository;"))
    private static GameProfileRepository lunararc$repository(MinecraftServicesDiscoveryService discovery, Operation<GameProfileRepository> original) {
        return LunarArcProfileServices.wrap(original.call(discovery));
    }

    @WrapOperation(method = "create", require = 1,
            at = @At(value = "INVOKE", target = "Lcom/mojang/authlib/services/MinecraftServicesDiscoveryService;createMinecraftSessionService()Lcom/mojang/authlib/minecraft/SessionService;"))
    private static SessionService lunararc$session(MinecraftServicesDiscoveryService discovery, Operation<SessionService> original) {
        return LunarArcProfileServices.wrap(original.call(discovery));
    }
}
