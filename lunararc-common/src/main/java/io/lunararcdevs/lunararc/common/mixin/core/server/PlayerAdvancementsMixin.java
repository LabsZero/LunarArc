package io.lunararcdevs.lunararc.common.mixin.core.server;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.lunararcdevs.lunararc.common.event.LunarArcAdvancementEvents;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;
import java.util.function.Consumer;

@Mixin(PlayerAdvancements.class)
public abstract class PlayerAdvancementsMixin {
    @Shadow private ServerPlayer player;

    @Unique private LunarArcAdvancementEvents.Announcement lunararc$announcement;

    @WrapOperation(
            method = "award(Lnet/minecraft/advancements/AdvancementHolder;Ljava/lang/String;)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/advancements/AdvancementProgress;grantProgress(Ljava/lang/String;)Z"),
            require = 0)
    private boolean lunararc$criterionGrant(AdvancementProgress progress, String criterion, Operation<Boolean> original,
                                            @Local(argsOnly = true) AdvancementHolder advancement) {
        boolean granted = original.call(progress, criterion);
        if (!granted) return false;
        if (LunarArcAdvancementEvents.allowCriterion(this.player, advancement, criterion)) return true;
        progress.revokeProgress(criterion);
        return false;
    }

    @WrapOperation(
            method = "award(Lnet/minecraft/advancements/AdvancementHolder;Ljava/lang/String;)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/advancements/AdvancementRewards;grant(Lnet/minecraft/server/level/ServerPlayer;)V"),
            require = 0)
    private void lunararc$advancementDone(AdvancementRewards rewards, ServerPlayer rewardee, Operation<Void> original,
                                          @Local(argsOnly = true) AdvancementHolder advancement) {
        this.lunararc$announcement = LunarArcAdvancementEvents.fireDone(this.player, advancement);
        original.call(rewards, rewardee);
    }

    @WrapOperation(
            method = "award(Lnet/minecraft/advancements/AdvancementHolder;Ljava/lang/String;)Z",
            at = @At(value = "INVOKE", target = "Ljava/util/Optional;ifPresent(Ljava/util/function/Consumer;)V"),
            require = 0)
    private void lunararc$scopeAnnouncement(Optional<?> display, Consumer<?> announce, Operation<Void> original) {
        LunarArcAdvancementEvents.begin(this.lunararc$announcement);
        try {
            original.call(display, announce);
        } finally {
            LunarArcAdvancementEvents.end();
            this.lunararc$announcement = null;
        }
    }
}
