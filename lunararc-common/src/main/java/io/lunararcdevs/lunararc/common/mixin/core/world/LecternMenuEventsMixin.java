package io.lunararcdevs.lunararc.common.mixin.core.world;

import io.lunararcdevs.lunararc.common.event.LunarArcMoreEvents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.LecternMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LecternMenu.class)
public abstract class LecternMenuEventsMixin {
    @Shadow @Final private Container lectern;
    @Shadow @Final private ContainerData lecternData;

    @Inject(method = "clickMenuButton", at = @At("HEAD"), cancellable = true, require = 0)
    private void lunararc$click(Player player, int id, CallbackInfoReturnable<Boolean> cir) {
        boolean allowed;
        if (id == 3) {
            allowed = LunarArcMoreEvents.lecternTake(player, this.lectern);
        } else if (id == 1 || id == 2) {
            int old = this.lecternData.get(0);
            allowed = LunarArcMoreEvents.lecternPage(player, this.lectern, old, id == 2 ? old + 1 : old - 1);
        } else if (id >= 100) {
            allowed = LunarArcMoreEvents.lecternPage(player, this.lectern, this.lecternData.get(0), id - 100);
        } else {
            return;
        }
        if (!allowed) cir.setReturnValue(false);
    }
}
