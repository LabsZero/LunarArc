package io.lunararcdevs.lunararc.common.mixin.core.events;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.lunararcdevs.lunararc.common.bridge.AbstractContainerMenuBridge;
import io.lunararcdevs.lunararc.common.event.LunarArcExtraEvents;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/** PlayerChangeBeaconEffectEvent: a plugin may cancel, change the chosen effects, or keep the payment item. */
@Mixin(BeaconMenu.class)
public abstract class ExtraBeaconEffectEventMixin {
    @Shadow @Final private ContainerLevelAccess access;
    @Shadow @Final private ContainerData beaconData;

    // The payment slot is the menu's first slot; its declared type is private to vanilla, so go through the slot list.
    @org.spongepowered.asm.mixin.Unique
    private net.minecraft.world.inventory.Slot lunararc$payment() {
        return ((AbstractContainerMenu) (Object) this).slots.get(0);
    }

    @WrapMethod(method = "updateEffects", require = 0)
    private boolean lunararc$beaconEffects(Optional<Holder<MobEffect>> primary, Optional<Holder<MobEffect>> secondary, Operation<Boolean> original) {
        var owner = ((AbstractContainerMenuBridge) (AbstractContainerMenu) (Object) this).lunararc$getOwner();
        if (owner == null || !this.lunararc$payment().hasItem()) {
            return original.call(primary, secondary);
        }
        org.bukkit.block.Block beacon = this.access.evaluate((Level level, BlockPos pos) ->
                level instanceof ServerLevel server ? (org.bukkit.block.Block) CraftBlock.at(server, pos) : null).orElse(null);
        LunarArcExtraEvents.BeaconChoice choice = LunarArcExtraEvents.beaconEffects(owner, beacon, primary, secondary);
        if (choice == null) return false;
        this.beaconData.set(1, BeaconMenu.encodeEffect(choice.primary().orElse(null)));
        this.beaconData.set(2, BeaconMenu.encodeEffect(choice.secondary().orElse(null)));
        if (choice.consume()) this.lunararc$payment().remove(1);
        this.access.execute(Level::blockEntityChanged);
        return true;
    }
}
